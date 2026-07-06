(() => {
    const TIME_LIMIT_SEC = 5;
    const MAX_SUBMIT_SEC = 4.9;
    const TIMINGS = [
        { value: 'TOO_EARLY', label: '너무 이르게' },
        { value: 'EARLY', label: '이르게' },
        { value: 'NORMAL', label: '보통' },
        { value: 'LATE', label: '늦게' },
        { value: 'TOO_LATE', label: '너무 늦게' },
    ];

    const { getMatchSessionId, goWithMatch, mountInGameHud } = window.BluffBallNav;

    const startCoordinateEl = document.getElementById('startCoordinate');
    const timerDisplay = document.getElementById('timerDisplay');
    const coordGrid = document.getElementById('coordGrid');
    const btnCoordZero = document.getElementById('btnCoordZero');
    const timingGrid = document.getElementById('timingGrid');
    const timingRail = document.getElementById('timingRail');
    const resultWaitPanel = document.getElementById('resultWaitPanel');

    let startCoordinate = null;
    let timerStartedAt = null;
    let timerId = null;
    let selectedCoordinate = null;
    let selectedTiming = null;
    let submitting = false;
    let timedOutHandled = false;

    function getElapsedSec() {
        if (!timerStartedAt) return 0;
        return (Date.now() - timerStartedAt) / 1000;
    }

    function getSubmitResponseTimeSec() {
        return Math.min(Math.max(0.1, Math.round(getElapsedSec() * 10) / 10), MAX_SUBMIT_SEC);
    }

    function isTimedOut() {
        return getElapsedSec() > TIME_LIMIT_SEC;
    }

    function updateTimerDisplay() {
        const remaining = TIME_LIMIT_SEC - getElapsedSec();
        if (remaining > 0) {
            timerDisplay.textContent = `${remaining.toFixed(1)}s`;
            timerDisplay.className = 'timer-value';
            return;
        }
        timerDisplay.textContent = '0.0s';
        timerDisplay.className = 'timer-value overtime';
        if (!timedOutHandled) {
            timedOutHandled = true;
            handleTimeout();
        }
    }

    function startTimer() {
        timerStartedAt = Date.now();
        sessionStorage.setItem('bluffball.batterTimerStartedAt', String(timerStartedAt));
        if (timerId) clearInterval(timerId);
        timerId = setInterval(updateTimerDisplay, 100);
        updateTimerDisplay();
    }

    function lockInputs() {
        coordGrid.querySelectorAll('button').forEach((btn) => { btn.disabled = true; });
        timingGrid.querySelectorAll('button').forEach((btn) => { btn.disabled = true; });
        if (btnCoordZero) btnCoordZero.disabled = true;
    }

    function showWaitingForResult() {
        if (resultWaitPanel) {
            resultWaitPanel.hidden = false;
        }
        lockInputs();
    }

    function showTimingRail() {
        if (!timingRail) {
            return;
        }
        timingRail.hidden = false;
        timingRail.classList.add('is-visible');
    }

    function hideTimingRail() {
        if (!timingRail) {
            return;
        }
        timingRail.classList.remove('is-visible');
        timingRail.hidden = true;
    }

    function renderTimingGrid() {
        timingGrid.innerHTML = '';
        TIMINGS.forEach((t) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'timing-btn';
            if (selectedTiming === t.value) {
                btn.classList.add('selected');
            }
            btn.textContent = t.label;
            btn.disabled = submitting || selectedCoordinate === null;
            btn.addEventListener('click', () => selectTiming(t.value));
            timingGrid.appendChild(btn);
        });
    }

    function renderCoordGrid() {
        coordGrid.innerHTML = '';
        for (let n = 1; n <= 25; n++) {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'coord-btn';
            const x = (n - 1) % 5;
            const y = Math.floor((n - 1) / 5);
            if (x >= 1 && x <= 3 && y >= 1 && y <= 3) {
                btn.classList.add('strike-zone');
            }
            if (n === startCoordinate) {
                btn.classList.add('pitcher-start');
            }
            if (selectedCoordinate === n) {
                btn.classList.add('selected');
            }
            btn.textContent = n;
            btn.disabled = submitting || startCoordinate === null || selectedCoordinate !== null;
            btn.addEventListener('click', () => selectCoordinate(n));
            coordGrid.appendChild(btn);
        }
        if (btnCoordZero) {
            btnCoordZero.classList.toggle('selected', selectedCoordinate === 0);
            btnCoordZero.disabled = submitting || startCoordinate === null || selectedCoordinate !== null;
        }
    }

    function refreshInputs() {
        renderCoordGrid();
        renderTimingGrid();
    }

    function trySubmitIfReady() {
        if (selectedCoordinate === null || selectedTiming === null || submitting) {
            return;
        }
        if (isTimedOut()) {
            submitTimeout();
            return;
        }
        submitPayload({
            responseTimeSec: getSubmitResponseTimeSec(),
            batterCoordinateNumber: selectedCoordinate,
            timing: selectedTiming,
        });
    }

    function submitPayload(payload) {
        if (submitting) return;
        submitting = true;
        if (timerId) clearInterval(timerId);

        if (!BluffBallGameWs.isConnected()) {
            submitting = false;
            return;
        }

        BluffBallGameWs.publish('batter/select-card', payload);
        showWaitingForResult();
    }

    function selectTiming(timing) {
        if (submitting || startCoordinate === null) return;
        selectedTiming = timing;
        refreshInputs();
        trySubmitIfReady();
    }

    function selectCoordinate(n) {
        if (submitting || startCoordinate === null || selectedCoordinate !== null) return;

        selectedCoordinate = n;
        sessionStorage.setItem('bluffball.batterCoordinate', String(n));
        showTimingRail();
        refreshInputs();
        trySubmitIfReady();
    }

    function submitTimeout() {
        const coord = selectedCoordinate ?? 0;
        submitPayload({
            responseTimeSec: TIME_LIMIT_SEC + 0.1,
            batterCoordinateNumber: coord,
            timing: null,
        });
    }

    function handleTimeout() {
        lockInputs();
        submitTimeout();
    }

    function beginBatterTurn(coordinateNumber) {
        startCoordinate = coordinateNumber;
        sessionStorage.setItem('bluffball.startCoordinate', String(startCoordinate));
        startCoordinateEl.textContent = String(startCoordinate);
        sessionStorage.removeItem('bluffball.batterCoordinate');
        timedOutHandled = false;
        submitting = false;
        selectedCoordinate = null;
        selectedTiming = null;
        hideTimingRail();
        refreshInputs();
        startTimer();
    }

    function handlePitcherReady({ event }) {
        beginBatterTurn(event.startCoordinateNumber);
    }

    function handleTurnResult() {
        goWithMatch('/game-test/BatterResult.html');
    }

    function handleGameEnd() {
        goWithMatch('/game-test/GameEnd.html');
    }

    function init() {
        if (!BluffBallRole.requireLoginOrRedirect()) {
            return;
        }
        if (!BluffBallRole.isBatter()) {
            goWithMatch('/game-test/BatterWait.html');
            return;
        }

        const matchSessionId = getMatchSessionId();
        if (!matchSessionId) {
            goWithMatch('/game-test/Home.html');
            return;
        }

        const storedStart = sessionStorage.getItem('bluffball.startCoordinate');
        if (!storedStart) {
            goWithMatch('/game-test/BatterWait.html');
            return;
        }

        renderTimingGrid();
        renderCoordGrid();

        BluffBallGameWs.on(BluffBallGameWs.EVENT.PITCHER_READY, handlePitcherReady);
        BluffBallGameWs.on(BluffBallGameWs.EVENT.TURN_RESULT, handleTurnResult);
        BluffBallGameWs.on(BluffBallGameWs.EVENT.GAME_END, handleGameEnd);

        BluffBallGameWs.connect({
            matchSessionId,
            reconnectDelay: 5000,
        });

        beginBatterTurn(Number(storedStart));
        mountInGameHud();
    }

    btnCoordZero?.addEventListener('click', () => selectCoordinate(0));
    init();
})();
