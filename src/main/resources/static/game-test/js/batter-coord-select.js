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

    const { getMatchSessionId, goWithMatch, mountBroadcastHud } = window.BluffBallNav;

    const startCoordinateEl = document.getElementById('startCoordinate');
    const timerDisplay = document.getElementById('timerDisplay');
    const coordGrid = document.getElementById('coordGrid');
    const btnCoordZero = document.getElementById('btnCoordZero');
    const timingPanel = document.getElementById('timingPanel');
    const timingGrid = document.getElementById('timingGrid');

    let startCoordinate = null;
    let timerStartedAt = null;
    let timerId = null;
    let selectedCoordinate = null;
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
        if (timerId) clearInterval(timerId);
        timerId = setInterval(updateTimerDisplay, 100);
        updateTimerDisplay();
    }

    function lockCoordSelection() {
        coordGrid.querySelectorAll('button').forEach((btn) => { btn.disabled = true; });
        if (btnCoordZero) btnCoordZero.disabled = true;
    }

    function showTimingPanel() {
        timingPanel.hidden = false;
        timingPanel.classList.add('is-visible');
    }

    function renderTimingGrid() {
        timingGrid.innerHTML = '';
        TIMINGS.forEach((t) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'timing-btn';
            btn.textContent = t.label;
            btn.addEventListener('click', () => submitTiming(t.value));
            timingGrid.appendChild(btn);
        });
    }

    async function submitPayload(payload) {
        if (submitting) return;
        submitting = true;
        if (timerId) clearInterval(timerId);

        const matchSessionId = getMatchSessionId();
        try {
            const res = await fetch(`/game-test/api/match/${matchSessionId}/batter/select-card`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });
            if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
            }
            const data = await res.json();
            sessionStorage.setItem('bluffball.batterResult', JSON.stringify(data));
            if (data.gameOver) {
                sessionStorage.setItem('bluffball.gameEnd', JSON.stringify(data));
            }
            goWithMatch('/game-test/BatterResult.html');
        } catch (_) {
            submitting = false;
            goWithMatch('/game-test/PitcherSelect.html');
        }
    }

    function submitTiming(timing) {
        if (selectedCoordinate === null) return;

        if (isTimedOut()) {
            submitTimeout();
            return;
        }

        submitPayload({
            responseTimeSec: getSubmitResponseTimeSec(),
            batterCoordinateNumber: selectedCoordinate,
            timing,
        });
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
        lockCoordSelection();
        timingPanel.hidden = true;
        submitTimeout();
    }

    function selectCoordinate(n) {
        if (submitting || selectedCoordinate !== null) return;

        selectedCoordinate = n;
        renderCoordGrid();
        lockCoordSelection();
        showTimingPanel();

        if (isTimedOut()) {
            submitTimeout();
        }
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
            btn.addEventListener('click', () => selectCoordinate(n));
            coordGrid.appendChild(btn);
        }
        if (btnCoordZero) {
            btnCoordZero.classList.toggle('selected', selectedCoordinate === 0);
            btnCoordZero.disabled = selectedCoordinate !== null || submitting;
        }
    }

    async function init() {
        const matchSessionId = getMatchSessionId();
        if (!matchSessionId) {
            goWithMatch('/game-test/Home.html');
            return;
        }

        const storedStart = sessionStorage.getItem('bluffball.startCoordinate');
        if (storedStart) {
            startCoordinate = Number(storedStart);
        } else {
            const res = await fetch(`/game-test/api/match/${matchSessionId}/prepare-batter`);
            if (!res.ok) {
                throw new Error('prepare-batter failed');
            }
            const data = await res.json();
            startCoordinate = data.startCoordinateNumber;
            sessionStorage.setItem('bluffball.startCoordinate', String(startCoordinate));
        }

        startCoordinateEl.textContent = String(startCoordinate);
        sessionStorage.removeItem('bluffball.batterCoordinate');
        sessionStorage.removeItem('bluffball.batterTimerStartedAt');

        renderTimingGrid();
        renderCoordGrid();
        startTimer();
        mountBroadcastHud();
    }

    btnCoordZero?.addEventListener('click', () => selectCoordinate(0));
    init().catch(() => goWithMatch('/game-test/PitcherSelect.html'));
})();
