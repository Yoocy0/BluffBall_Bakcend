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

    const { getMatchSessionId, goWithMatch, mountBroadcastHud, renderBroadcastHud } = window.BluffBallNav;

    const startCoordinateEl = document.getElementById('startCoordinate');
    const timerDisplay = document.getElementById('timerDisplay');
    const timingGrid = document.getElementById('timingGrid');

    let timerStartedAt = null;
    let timerId = null;
    let submitting = false;

    function getElapsedSec() {
        if (!timerStartedAt) return 0;
        return (Date.now() - timerStartedAt) / 1000;
    }

    function getSubmitResponseTimeSec() {
        const elapsed = getElapsedSec();
        return Math.min(Math.max(0.1, Math.round(elapsed * 10) / 10), MAX_SUBMIT_SEC);
    }

    function updateTimerDisplay() {
        const elapsed = getElapsedSec();
        const remaining = TIME_LIMIT_SEC - elapsed;
        if (remaining > 0) {
            timerDisplay.textContent = `${remaining.toFixed(1)}s`;
            timerDisplay.className = 'timer-value';
        } else {
            timerDisplay.textContent = `+${(elapsed - TIME_LIMIT_SEC).toFixed(1)}s`;
            timerDisplay.className = 'timer-value overtime';
        }
    }

    function startTimer() {
        if (timerId) clearInterval(timerId);
        timerId = setInterval(updateTimerDisplay, 100);
        updateTimerDisplay();
    }

    async function submitTiming(timing) {
        if (submitting) return;
        submitting = true;

        const matchSessionId = getMatchSessionId();
        const coordRaw = sessionStorage.getItem('bluffball.batterCoordinate');
        if (coordRaw == null) {
            goWithMatch('/game-test/BatterCoordSelect.html');
            return;
        }

        const payload = {
            responseTimeSec: getSubmitResponseTimeSec(),
            batterCoordinateNumber: Number(coordRaw),
            timing,
        };

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
            goWithMatch('/game-test/BatterCoordSelect.html');
        }
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

    function init() {
        const matchSessionId = getMatchSessionId();
        const coordRaw = sessionStorage.getItem('bluffball.batterCoordinate');
        const startRaw = sessionStorage.getItem('bluffball.startCoordinate');
        const startedRaw = sessionStorage.getItem('bluffball.batterTimerStartedAt');

        if (!matchSessionId || coordRaw == null) {
            goWithMatch('/game-test/BatterCoordSelect.html');
            return;
        }

        startCoordinateEl.textContent = startRaw ?? '-';
        timerStartedAt = startedRaw ? Number(startedRaw) : Date.now();
        sessionStorage.setItem('bluffball.batterTimerStartedAt', String(timerStartedAt));

        renderTimingGrid();
        startTimer();
        mountBroadcastHud();
    }

    init();
})();
