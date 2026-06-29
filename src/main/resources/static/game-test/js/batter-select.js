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

    const state = {
        startCoordinate: null,
        selectedCoordinate: null,
        selectedTiming: null,
        timerStartedAt: null,
        elapsedSec: 0,
        timerId: null,
        timedOut: false,
    };

    const els = {
        matchSessionId: document.getElementById('matchSessionId'),
        linkPitcherResult: document.getElementById('linkPitcherResult'),
        gameStatus: document.getElementById('gameStatus'),
        startCoordinate: document.getElementById('startCoordinate'),
        timerDisplay: document.getElementById('timerDisplay'),
        timerHint: document.getElementById('timerHint'),
        coordGrid: document.getElementById('coordGrid'),
        btnCoordZero: document.getElementById('btnCoordZero'),
        timingGrid: document.getElementById('timingGrid'),
        btnSubmit: document.getElementById('btnSubmit'),
        log: document.getElementById('log'),
    };

    function log(message, type = '') {
        const line = document.createElement('div');
        if (type) line.className = type;
        line.textContent = `[${new Date().toLocaleTimeString()}] ${message}`;
        els.log.prepend(line);
    }

    function getMatchSessionId() {
        return els.matchSessionId.value.trim();
    }

    function readQueryMatchId() {
        const params = new URLSearchParams(window.location.search);
        return params.get('matchSessionId') || sessionStorage.getItem('bluffball.matchSessionId') || '';
    }

    function getElapsedSec() {
        if (!state.timerStartedAt) return 0;
        return (Date.now() - state.timerStartedAt) / 1000;
    }

    /** 서버 전송용 — 항상 t < 5 (타격 이벤트 테스트) */
    function getSubmitResponseTimeSec() {
        const elapsed = getElapsedSec();
        return Math.min(Math.max(0.1, Math.round(elapsed * 10) / 10), MAX_SUBMIT_SEC);
    }

    function updateTimerDisplay() {
        state.elapsedSec = getElapsedSec();
        const remaining = TIME_LIMIT_SEC - state.elapsedSec;

        if (remaining > 0) {
            els.timerDisplay.textContent = `${remaining.toFixed(1)}s 남음`;
            els.timerDisplay.className = 'timer-value';
        } else {
            if (!state.timedOut) {
                state.timedOut = true;
                log('5초 UI 초과 — 제출 시 responseTimeSec은 4.9s로 전송 (t < 5)', 'ok');
            }
            els.timerDisplay.textContent = `+${(state.elapsedSec - TIME_LIMIT_SEC).toFixed(1)}s (UI 초과)`;
            els.timerDisplay.className = 'timer-value overtime';
        }
        updateSubmitButton();
    }

    function startTimer() {
        state.timerStartedAt = Date.now();
        state.timedOut = false;
        if (state.timerId) clearInterval(state.timerId);
        state.timerId = setInterval(updateTimerDisplay, 100);
        updateTimerDisplay();
        log('5초 타이머 시작', 'ok');
    }

    function updateSubmitButton() {
        const hasCoord = state.selectedCoordinate !== null;
        const hasTiming = !!state.selectedTiming;
        els.btnSubmit.disabled = !getMatchSessionId() || !hasCoord || !hasTiming;
    }

    async function loadGameStatus() {
        const matchSessionId = getMatchSessionId();
        if (!matchSessionId || !els.gameStatus) return;

        try {
            const res = await fetch(`/game-test/api/match/${matchSessionId}/game-status`);
            if (!res.ok) return;
            const s = await res.json();
            els.gameStatus.textContent =
                `턴 ${s.turnNumber} · ${s.inning}회 ${s.isTop ? '초' : '말'} · `
                + `B${s.balls} S${s.strikes} O${s.outs} · ${s.awayScore}:${s.homeScore}`
                + (s.gameOver ? ' · 경기 종료' : '');
        } catch (_) {
            /* ignore */
        }
    }

    function renderCoordGrid() {
        els.coordGrid.innerHTML = '';
        for (let n = 1; n <= 25; n++) {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'coord-btn';
            const x = (n - 1) % 5;
            const y = Math.floor((n - 1) / 5);
            if (x >= 1 && x <= 3 && y >= 1 && y <= 3) {
                btn.classList.add('strike-zone');
            }
            if (n === state.startCoordinate) {
                btn.classList.add('pitcher-start');
            }
            if (state.selectedCoordinate === n) {
                btn.classList.add('selected');
            }
            btn.textContent = n;
            btn.addEventListener('click', () => {
                state.selectedCoordinate = n;
                renderCoordGrid();
                updateSubmitButton();
                log(`좌표 선택: ${n}`);
            });
            els.coordGrid.appendChild(btn);
        }

        els.btnCoordZero.classList.toggle('selected', state.selectedCoordinate === 0);
    }

    function renderTimingGrid() {
        els.timingGrid.innerHTML = '';
        TIMINGS.forEach((t) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'timing-btn';
            if (state.selectedTiming === t.value) {
                btn.classList.add('selected');
            }
            btn.textContent = t.label;
            btn.addEventListener('click', () => {
                state.selectedTiming = t.value;
                renderTimingGrid();
                updateSubmitButton();
                log(`타이밍: ${t.label}`);
            });
            els.timingGrid.appendChild(btn);
        });
    }

    els.btnCoordZero.addEventListener('click', () => {
        state.selectedCoordinate = 0;
        renderCoordGrid();
        updateSubmitButton();
        log('좌표 선택: 0 (폭투 존)');
    });

    async function loadPrepare() {
        const matchSessionId = getMatchSessionId();
        if (!matchSessionId) {
            log('matchSessionId를 입력하세요.', 'err');
            return;
        }

        await loadGameStatus();

        try {
            const res = await fetch(`/game-test/api/match/${matchSessionId}/prepare-batter`);
            if (!res.ok) {
                const text = await res.text();
                throw new Error(`HTTP ${res.status} — ${text}`);
            }
            const data = await res.json();
            state.startCoordinate = data.startCoordinateNumber;
            els.startCoordinate.textContent = String(data.startCoordinateNumber);
            renderCoordGrid();
            startTimer();
            log(`시작 좌표 ${data.startCoordinateNumber} 로드`, 'ok');
        } catch (e) {
            log(`준비 실패: ${e.message}`, 'err');
        }
    }

    async function submitSelection() {
        const matchSessionId = getMatchSessionId();
        const responseTimeSec = getSubmitResponseTimeSec();

        const payload = {
            responseTimeSec,
            batterCoordinateNumber: state.selectedCoordinate,
            timing: state.selectedTiming,
        };

        try {
            const res = await fetch(`/game-test/api/match/${matchSessionId}/batter/select-card`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });
            if (!res.ok) {
                const text = await res.text();
                throw new Error(`HTTP ${res.status} — ${text}`);
            }
            const data = await res.json();
            log(`제출 — ${data.turnResult} (전송 ${responseTimeSec}s < 5)`, 'ok');

            const qs = new URLSearchParams({ matchSessionId });

            if (data.gameOver) {
                sessionStorage.setItem('bluffball.gameEnd', JSON.stringify(data));
                sessionStorage.setItem('bluffball.batterResult', JSON.stringify(data));
                window.location.href = `/game-test/GameEnd.html?${qs.toString()}`;
                return;
            }

            sessionStorage.setItem('bluffball.batterResult', JSON.stringify(data));
            window.location.href = `/game-test/BatterResult.html?${qs.toString()}`;
        } catch (e) {
            log(`제출 실패: ${e.message}`, 'err');
        }
    }

    els.btnSubmit.addEventListener('click', submitSelection);

    renderTimingGrid();

    const initialMatchId = readQueryMatchId();
    if (initialMatchId) {
        els.matchSessionId.value = initialMatchId;
        if (els.linkPitcherResult) {
            els.linkPitcherResult.href = `/game-test/PitcherSelect.html?matchSessionId=${encodeURIComponent(initialMatchId)}&loop=1`;
        }
        loadPrepare();
    } else {
        log('matchSessionId가 없습니다.');
    }
})();
