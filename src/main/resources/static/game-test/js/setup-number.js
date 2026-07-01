(() => {
    const PHASES = [
        { key: 'out', label: '아웃 번호', desc: '5개 선택 (1~12)', max: 5 },
        { key: 'dp', label: '병살 번호', desc: '1개 선택 — 아웃 번호와 중복 불가', max: 1 },
        { key: 'triple', label: '3루타 번호', desc: '1개 선택', max: 1 },
        { key: 'hr', label: '홈런 번호', desc: '1개 선택 — 3루타 번호와 중복 불가', max: 1 },
    ];

    const state = {
        matchSessionId: '',
        phaseIndex: 0,
        outNumList: [],
        dpNumList: [],
        tripleNumList: [],
        hrNumList: [],
        submitted: false,
    };

    const els = {
        matchSessionLabel: document.getElementById('matchSessionLabel'),
        setupStatus: document.getElementById('setupStatus'),
        phaseTitle: document.getElementById('phaseTitle'),
        phaseDesc: document.getElementById('phaseDesc'),
        numberGrid: document.getElementById('numberGrid'),
        summaryOut: document.getElementById('summaryOut'),
        summaryDp: document.getElementById('summaryDp'),
        summaryTriple: document.getElementById('summaryTriple'),
        summaryHr: document.getElementById('summaryHr'),
        btnReset: document.getElementById('btnReset'),
        btnSubmit: document.getElementById('btnSubmit'),
        log: document.getElementById('log'),
    };

    function log(message, type = '') {
        const line = document.createElement('div');
        if (type) {
            line.className = type;
        }
        line.textContent = `[${new Date().toLocaleTimeString()}] ${message}`;
        els.log.prepend(line);
    }

    function setSetupStatus(text, isOk = false) {
        if (!els.setupStatus) {
            return;
        }
        els.setupStatus.textContent = text;
        els.setupStatus.style.color = isOk ? '#81c995' : '#9aa0a6';
    }

    /** URL·sessionStorage에서 matchSessionId 로드 */
    function resolveMatchSessionId() {
        const params = new URLSearchParams(window.location.search);
        return params.get('matchSessionId')
            || sessionStorage.getItem('bluffball.matchSessionId')
            || '';
    }

    function requireLoginOrRedirect() {
        if (!window.BluffBallAuth?.isLoggedIn?.()) {
            window.location.href = '/game-test/Home.html';
            return false;
        }
        return true;
    }

    function currentPhase() {
        return PHASES[state.phaseIndex];
    }

    function listForPhase(key) {
        switch (key) {
            case 'out': return state.outNumList;
            case 'dp': return state.dpNumList;
            case 'triple': return state.tripleNumList;
            case 'hr': return state.hrNumList;
            default: return [];
        }
    }

    function isNumberDisabled(num) {
        const phase = currentPhase();
        const list = listForPhase(phase.key);

        if (list.includes(num)) {
            return false;
        }
        if (list.length >= phase.max) {
            return true;
        }
        if (phase.key === 'dp' && state.outNumList.includes(num)) {
            return true;
        }
        if (phase.key === 'hr' && state.tripleNumList.includes(num)) {
            return true;
        }
        return false;
    }

    function isNumberSelected(num) {
        return listForPhase(currentPhase().key).includes(num);
    }

    function renderPhase() {
        const phase = currentPhase();
        const list = listForPhase(phase.key);
        els.phaseTitle.textContent = `${phase.label} (${list.length}/${phase.max})`;
        els.phaseDesc.textContent = phase.desc;

        els.numberGrid.innerHTML = '';
        for (let n = 1; n <= 12; n++) {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'num-btn';
            btn.textContent = n;

            if (isNumberSelected(n)) {
                btn.classList.add('selected');
            }
            if (isNumberDisabled(n)) {
                btn.disabled = true;
            }

            btn.addEventListener('click', () => toggleNumber(n));
            els.numberGrid.appendChild(btn);
        }

        renderSummary();
        updateActions();
    }

    function renderSummary() {
        els.summaryOut.textContent = formatList(state.outNumList, 5);
        els.summaryDp.textContent = formatList(state.dpNumList, 1);
        els.summaryTriple.textContent = formatList(state.tripleNumList, 1);
        els.summaryHr.textContent = formatList(state.hrNumList, 1);
    }

    function formatList(list, expected) {
        if (list.length === 0) {
            return '-';
        }
        const text = list.join(', ');
        return expected > 1 ? `${text} (${list.length}/${expected})` : text;
    }

    function toggleNumber(num) {
        const phase = currentPhase();
        const list = listForPhase(phase.key);
        const idx = list.indexOf(num);

        if (idx >= 0) {
            list.splice(idx, 1);
            renderPhase();
            return;
        }

        if (list.length >= phase.max) {
            return;
        }

        list.push(num);
        list.sort((a, b) => a - b);

        if (list.length >= phase.max && state.phaseIndex < PHASES.length - 1) {
            state.phaseIndex++;
            log(`${phase.label} 선택 완료 → ${PHASES[state.phaseIndex].label} 단계`);
        }

        renderPhase();
    }

    function isSelectionComplete() {
        return state.outNumList.length === 5
            && state.dpNumList.length === 1
            && state.tripleNumList.length === 1
            && state.hrNumList.length === 1;
    }

    function buildPayload() {
        return {
            outNumList: [...state.outNumList],
            dpNumList: [...state.dpNumList],
            tripleNumList: [...state.tripleNumList],
            hrNumList: [...state.hrNumList],
        };
    }

    function updateActions() {
        const allDone = isSelectionComplete();
        els.btnSubmit.disabled = !allDone || !BluffBallGameWs.isConnected() || state.submitted;
    }

    function goToMulligan() {
        sessionStorage.setItem('bluffball.matchSessionId', state.matchSessionId);
        window.location.href =
            `/game-test/Mulligan.html?matchSessionId=${encodeURIComponent(state.matchSessionId)}`;
    }

    function goToBatterWait() {
        sessionStorage.setItem('bluffball.matchSessionId', state.matchSessionId);
        window.location.href =
            `/game-test/BatterWait.html?matchSessionId=${encodeURIComponent(state.matchSessionId)}`;
    }

    function goToBatterPlay(startCoordinateNumber) {
        sessionStorage.setItem('bluffball.matchSessionId', state.matchSessionId);
        sessionStorage.setItem('bluffball.startCoordinate', String(startCoordinateNumber));
        window.location.href =
            `/game-test/BatterCoordSelect.html?matchSessionId=${encodeURIComponent(state.matchSessionId)}`;
    }

    function handleCardHandEvent({ event }) {
        if (!BluffBallGameWs.isCardHandForMe?.(event)) {
            return;
        }
        if (event?.pitcherUserId != null) {
            BluffBallRole.syncRoleFromPitcherUserId(event.pitcherUserId);
        }
        log('카드 패 수신 — 멀리건 화면으로 이동합니다.', 'ok');
        goToMulligan();
    }

    function handlePitcherReadyEvent({ event }) {
        if (!BluffBallRole.isBatter()) {
            return;
        }
        log(`PitcherReadyEvent — 시작 좌표 ${event.startCoordinateNumber}`, 'ok');
        goToBatterPlay(event.startCoordinateNumber);
    }

    function resetSelection() {
        if (state.submitted) {
            return;
        }
        state.phaseIndex = 0;
        state.outNumList = [];
        state.dpNumList = [];
        state.tripleNumList = [];
        state.hrNumList = [];
        renderPhase();
        log('선택 초기화');
    }

    function connectGameWebSocket() {
        try {
            BluffBallGameWs.connect({
                matchSessionId: state.matchSessionId,
                reconnectDelay: 5000,
                onConnect: () => {
                    setSetupStatus('연결됨 — 숫자를 선택하고 제출하세요', true);
                    log(`WebSocket 연결 — ${BluffBallGameWs.topicGame(state.matchSessionId)}`, 'ok');
                    updateActions();
                },
                onError: (frame) => {
                    setSetupStatus('WebSocket 오류');
                    log(`STOMP 오류: ${frame.headers['message'] || frame.body}`, 'err');
                    updateActions();
                },
                onDisconnect: () => {
                    setSetupStatus('연결 종료');
                    updateActions();
                },
            });

            BluffBallGameWs.on(BluffBallGameWs.EVENT.CARD_HAND, (payload) => {
                log(`[WS CardHandEvent] ${payload.rawBody}`, 'ok');
                handleCardHandEvent(payload);
            });

            BluffBallGameWs.on(BluffBallGameWs.EVENT.PITCHER_READY, (payload) => {
                log(`[WS PitcherReadyEvent] ${payload.rawBody}`, 'ok');
                handlePitcherReadyEvent(payload);
            });

            BluffBallGameWs.on('*', (type, { rawBody }) => {
                if (type !== BluffBallGameWs.EVENT.CARD_HAND) {
                    log(`[WS ${type}] ${rawBody}`, 'ok');
                }
            });
        } catch (e) {
            setSetupStatus(e.message);
            log(e.message, 'err');
        }
    }

    function submitSetupNumbers() {
        if (!isSelectionComplete()) {
            log('모든 숫자를 선택하세요.', 'err');
            return;
        }
        if (!BluffBallGameWs.isConnected()) {
            log('WebSocket 연결 후 제출하세요.', 'err');
            return;
        }

        try {
            BluffBallGameWs.publish('setup-numbers', buildPayload());
            state.submitted = true;
            setSetupStatus('제출 완료 — 상대방·카드 드로우 대기 중...', true);
            log('setup-numbers 제출 완료', 'ok');
            updateActions();
        } catch (e) {
            log(e.message, 'err');
        }
    }

    function init() {
        if (!requireLoginOrRedirect()) {
            return;
        }

        state.matchSessionId = resolveMatchSessionId();
        if (!state.matchSessionId) {
            window.location.href = '/game-test/Home.html';
            return;
        }

        if (els.matchSessionLabel) {
            els.matchSessionLabel.textContent = `matchSessionId: ${state.matchSessionId}`;
        }

        sessionStorage.setItem('bluffball.matchSessionId', state.matchSessionId);

        els.btnReset.addEventListener('click', resetSelection);
        els.btnSubmit.addEventListener('click', submitSetupNumbers);

        renderPhase();
        connectGameWebSocket();
    }

    init();
})();
