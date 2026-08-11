(() => {
    const PHASE_DEFS = {
        out: { key: 'out', label: '아웃 번호', desc: '5개 선택 (1~12)', max: 5 },
        dp: { key: 'dp', label: '병살 번호', desc: '1개 선택 — 아웃 번호와 중복 불가', max: 1 },
        triple: { key: 'triple', label: '3루타 번호', desc: '1개 선택', max: 1 },
        hr: { key: 'hr', label: '홈런 번호', desc: '1개 선택 (7~12) — 3루타 번호와 중복 불가', max: 1 },
    };

    const state = {
        matchSessionId: '',
        phases: [
            PHASE_DEFS.out,
            PHASE_DEFS.dp,
            PHASE_DEFS.triple,
            PHASE_DEFS.hr,
        ],
        setupKind: 'FULL',
        gameMode: '',
        phaseIndex: 0,
        outNumList: [],
        dpNumList: [],
        tripleNumList: [],
        hrNumList: [],
        submitted: false,
        navigating: false,
    };

    const els = {
        matchSessionLabel: document.getElementById('matchSessionLabel'),
        setupSubtitle: document.getElementById('setupSubtitle'),
        setupStatus: document.getElementById('setupStatus'),
        phaseTitle: document.getElementById('phaseTitle'),
        phaseDesc: document.getElementById('phaseDesc'),
        numberGrid: document.getElementById('numberGrid'),
        summaryOut: document.getElementById('summaryOut'),
        summaryDp: document.getElementById('summaryDp'),
        summaryTriple: document.getElementById('summaryTriple'),
        summaryHr: document.getElementById('summaryHr'),
        summaryOutRow: document.getElementById('summaryOutRow'),
        summaryDpRow: document.getElementById('summaryDpRow'),
        summaryTripleRow: document.getElementById('summaryTripleRow'),
        summaryHrRow: document.getElementById('summaryHrRow'),
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

    function phasesForKind(kind) {
        switch (kind) {
            case 'PITCHER':
                return [PHASE_DEFS.out, PHASE_DEFS.dp];
            case 'BATTER':
                return [PHASE_DEFS.triple, PHASE_DEFS.hr];
            default:
                return [PHASE_DEFS.out, PHASE_DEFS.dp, PHASE_DEFS.triple, PHASE_DEFS.hr];
        }
    }

    function subtitleForKind(kind) {
        switch (kind) {
            case 'PITCHER':
                return '투수 셋업 — 아웃 5 → 병살 1';
            case 'BATTER':
                return '타자 셋업 — 3루타 1 → 홈런 1';
            default:
                return '쇼다운 셋업 — 아웃 5 → 병살 1 → 3루타 1 → 홈런 1';
        }
    }

    function applySetupKind(kind) {
        state.setupKind = kind || 'FULL';
        state.phases = phasesForKind(state.setupKind);
        state.phaseIndex = 0;
        if (els.setupSubtitle) {
            els.setupSubtitle.textContent = subtitleForKind(state.setupKind);
        }
        const needs = new Set(state.phases.map((p) => p.key));
        if (els.summaryOutRow) {
            els.summaryOutRow.hidden = !needs.has('out');
        }
        if (els.summaryDpRow) {
            els.summaryDpRow.hidden = !needs.has('dp');
        }
        if (els.summaryTripleRow) {
            els.summaryTripleRow.hidden = !needs.has('triple');
        }
        if (els.summaryHrRow) {
            els.summaryHrRow.hidden = !needs.has('hr');
        }
    }

    function currentPhase() {
        return state.phases[state.phaseIndex];
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
        if (!phase) {
            return true;
        }
        const list = listForPhase(phase.key);

        if (list.includes(num)) {
            return false;
        }
        if (list.length >= phase.max) {
            return true;
        }
        if (phase.key === 'hr' && (num < 7 || num > 12)) {
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
        const phase = currentPhase();
        return phase ? listForPhase(phase.key).includes(num) : false;
    }

    function renderPhase() {
        const phase = currentPhase();
        if (!phase) {
            return;
        }
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
        if (!phase) {
            return;
        }
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

        if (list.length >= phase.max && state.phaseIndex < state.phases.length - 1) {
            state.phaseIndex++;
            log(`${phase.label} 선택 완료 → ${state.phases[state.phaseIndex].label} 단계`);
        }

        renderPhase();
    }

    function isSelectionComplete() {
        return state.phases.every((phase) => listForPhase(phase.key).length === phase.max);
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
        if (state.navigating) {
            return;
        }
        state.navigating = true;
        sessionStorage.setItem('bluffball.matchSessionId', state.matchSessionId);
        window.location.href =
            `/game-test/Mulligan.html?matchSessionId=${encodeURIComponent(state.matchSessionId)}`;
    }

    function goToBatterPlay(startCoordinateNumber) {
        if (state.navigating) {
            return;
        }
        state.navigating = true;
        sessionStorage.setItem('bluffball.matchSessionId', state.matchSessionId);
        sessionStorage.setItem('bluffball.startCoordinate', String(startCoordinateNumber));
        window.location.href =
            `/game-test/BatterCoordSelect.html?matchSessionId=${encodeURIComponent(state.matchSessionId)}`;
    }

    function goToLeaguePlay() {
        if (state.navigating) {
            return;
        }
        state.navigating = true;
        BluffBallNav.goToPlayAfterReady(state.matchSessionId);
    }

    function sleep(ms) {
        return new Promise((resolve) => setTimeout(resolve, ms));
    }

    /** 리그: 전원 셋업 완료 후 멀리건 없이 인게임으로 */
    async function waitAndEnterLeaguePlay() {
        setSetupStatus('제출 완료 — 다른 플레이어 셋업 대기 중...', true);
        for (let i = 0; i < 60; i++) {
            const session = await BluffBallNav.fetchSessionState(state.matchSessionId);
            if (session) {
                BluffBallNav.applySessionState(session);
                if (session.setupComplete
                    || session.phase === 'PITCHER_SELECT'
                    || session.phase === 'BATTER_SELECT') {
                    log('리그 셋업 완료 — 인게임으로 이동합니다.', 'ok');
                    goToLeaguePlay();
                    return;
                }
            }
            await sleep(500);
        }
        log('셋업 대기 시간 초과 — 현재 역할로 진행합니다.', 'err');
        goToLeaguePlay();
    }


    /** 쇼다운/봇: CardHand 미수신·드로우 실패 대비 폴링 */
    async function waitForShowdownCardDraw() {
        for (let i = 0; i < 40; i++) {
            if (state.navigating) {
                return;
            }
            const session = await BluffBallNav.fetchSessionState(state.matchSessionId);
            if (session) {
                BluffBallNav.applySessionState(session);
                const hand = session.myCardHand || [];
                if (hand.length > 0) {
                    log('카드 패 확인 — 멀리건 화면으로 이동합니다.', 'ok');
                    goToMulligan();
                    return;
                }
                if (session.setupComplete && i >= 6 && hand.length === 0) {
                    // 셋업은 끝났는데 핸드가 없으면 드로우 실패 가능성
                    setSetupStatus('카드 드로우 실패 — 구종 카드가 부족할 수 있습니다.');
                    log('셋업은 완료됐지만 카드 패가 없습니다. 구종 카드 보유를 확인하거나 봇전을 다시 시작해 보세요.', 'err');
                    return;
                }
            }
            await sleep(500);
        }
        if (!state.navigating) {
            setSetupStatus('카드 드로우 대기 시간 초과');
            log('카드 드로우 응답이 없습니다. 새로고침 후 다시 시도해 보세요.', 'err');
        }
    }

    function handleCardHandEvent({ event }) {
        if (!BluffBallGameWs.isCardHandForMe?.(event)) {
            return;
        }
        if (event?.pitcherUserId != null) {
            BluffBallRole.syncRoleFromPitcherUserId(event.pitcherUserId);
        }

        if (BluffBallNav.isLeagueMode(state.gameMode) || !BluffBallNav.usesInGameMulligan(state.gameMode)) {
            BluffBallGameWs.setAllMulliganReady(true);
            log('리그 핸드 수신 — 멀리건 없이 인게임으로 이동합니다.', 'ok');
            goToLeaguePlay();
            return;
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
            log('필요한 숫자를 모두 선택하세요.', 'err');
            return;
        }
        if (!BluffBallGameWs.isConnected()) {
            log('WebSocket 연결 후 제출하세요.', 'err');
            return;
        }

        try {
            const payload = buildPayload();
            BluffBallNav.persistMySetupNumbers(payload);
            BluffBallGameWs.publish('setup-numbers', payload);
            state.submitted = true;
            updateActions();

            if (BluffBallNav.isLeagueMode(state.gameMode) || !BluffBallNav.usesInGameMulligan(state.gameMode)) {
                log('setup-numbers 제출 완료 (리그)', 'ok');
                waitAndEnterLeaguePlay().catch((e) => log(e.message || String(e), 'err'));
            } else {
                setSetupStatus('제출 완료 — 상대방·카드 드로우 대기 중...', true);
                log('setup-numbers 제출 완료', 'ok');
                waitForShowdownCardDraw().catch((e) => log(e.message || String(e), 'err'));
            }
        } catch (e) {
            log(e.message, 'err');
        }
    }

    async function bootstrapFromSession() {
        const session = await BluffBallNav.fetchSessionState(state.matchSessionId);
        if (!session) {
            applySetupKind('FULL');
            log('세션 상태 조회 실패 — 쇼다운 셋업으로 진행', 'err');
            return;
        }

        BluffBallNav.applySessionState(session);
        state.gameMode = session.gameMode || '';
        applySetupKind(session.requiredSetupKind || 'FULL');
        log(`모드=${state.gameMode || '?'} · 셋업=${state.setupKind} · 역할=${session.myRole || '?'}`, 'ok');

        if (session.mySetupComplete && session.setupComplete) {
            log('이미 셋업 완료 — 인게임으로 이동합니다.', 'ok');
            goToLeaguePlay();
        }
    }

    async function init() {
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

        await bootstrapFromSession();
        if (state.navigating) {
            return;
        }

        renderPhase();
        connectGameWebSocket();
        if (window.BluffBallNav?.isLeagueMode?.()) {
            window.BluffBallPitcherSub?.mount?.({
                matchSessionId: state.matchSessionId,
                pollMs: 2500,
            });
        }
    }

    init();
})();
