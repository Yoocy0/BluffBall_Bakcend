(() => {
    const PAGE_VERSION = 'v5';

    const PHASES = [
        { key: 'out', label: '아웃 번호', desc: '5개 선택 (1~12)', max: 5 },
        { key: 'dp', label: '병살 번호', desc: '1개 선택 — 아웃 번호와 중복 불가', max: 1 },
        { key: 'triple', label: '3루타 번호', desc: '1개 선택', max: 1 },
        { key: 'hr', label: '홈런 번호', desc: '1개 선택 — 3루타 번호와 중복 불가', max: 1 },
    ];

    const state = {
        phaseIndex: 0,
        outNumList: [],
        dpNumList: [],
        tripleNumList: [],
        hrNumList: [],
        stompClient: null,
        connected: false,
    };

    const els = {
        pageVersion: document.getElementById('pageVersion'),
        matchSessionId: document.getElementById('matchSessionId'),
        wsStatus: document.getElementById('wsStatus'),
        btnConnect: document.getElementById('btnConnect'),
        btnDisconnect: document.getElementById('btnDisconnect'),
        btnCreateMatch: document.getElementById('btnCreateMatch'),
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

    function getMatchSessionId() {
        return els.matchSessionId.value.trim();
    }

    function updateActions() {
        const hasMatchId = !!getMatchSessionId();
        const allDone = isSelectionComplete();

        els.btnSubmit.disabled = !allDone || !hasMatchId;
    }

    function goToMulligan() {
        const matchSessionId = getMatchSessionId();
        if (!matchSessionId) {
            log('matchSessionId가 없습니다.', 'err');
            return;
        }
        sessionStorage.setItem('bluffball.matchSessionId', matchSessionId);
        window.location.href = `/game-test/Mulligan.html?matchSessionId=${encodeURIComponent(matchSessionId)}`;
    }

    function resetSelection() {
        state.phaseIndex = 0;
        state.outNumList = [];
        state.dpNumList = [];
        state.tripleNumList = [];
        state.hrNumList = [];
        renderPhase();
        log('선택 초기화');
    }

    function setConnected(connected) {
        state.connected = connected;
        els.wsStatus.textContent = connected ? '연결됨' : '미연결';
        els.wsStatus.className = `status ${connected ? 'connected' : 'disconnected'}`;
        els.btnConnect.disabled = connected;
        els.btnDisconnect.disabled = !connected;
    }

    function connectWebSocket() {
        const matchSessionId = getMatchSessionId();
        if (!matchSessionId) {
            log('matchSessionId를 입력하거나 「테스트 매치 생성」을 먼저 누르세요.', 'err');
            return;
        }

        if (typeof StompJs === 'undefined' || typeof SockJS === 'undefined') {
            log('STOMP/SockJS 라이브러리 로드 실패', 'err');
            return;
        }

        if (state.stompClient?.active) {
            state.stompClient.deactivate();
        }

        const client = new StompJs.Client({
            webSocketFactory: () => new SockJS('/ws'),
            reconnectDelay: 3000,
            onConnect: () => {
                setConnected(true);
                log(`WebSocket 연결 — /topic/game/${matchSessionId} 구독`, 'ok');

                client.subscribe(`/topic/game/${matchSessionId}`, (message) => {
                    log(`[WS 수신] ${message.body}`, 'ok');
                });
            },
            onStompError: (frame) => {
                log(`STOMP 오류: ${frame.headers['message'] || frame.body}`, 'err');
            },
            onWebSocketClose: () => {
                setConnected(false);
                log('WebSocket 연결 종료');
            },
        });

        client.onDisconnect = () => setConnected(false);
        client.activate();
        state.stompClient = client;
    }

    function disconnectWebSocket() {
        if (state.stompClient?.active) {
            state.stompClient.deactivate();
        }
        state.stompClient = null;
        setConnected(false);
        log('연결 해제');
    }

    function formatRedisResponse(data) {
        const userIds = Object.keys(data.outNumbers || {});
        if (userIds.length === 0) {
            return 'Redis: 저장된 숫자 없음';
        }
        const lines = userIds.map((uid) => {
            const out = (data.outNumbers[uid] || []).join(', ');
            const dp = (data.dpNumbers[uid] || []).join(', ');
            const triple = (data.tripleNumbers[uid] || []).join(', ');
            const hr = (data.hrNumbers[uid] || []).join(', ');
            return `userId=${uid} | out=[${out}] dp=[${dp}] triple=[${triple}] hr=[${hr}]`;
        });
        return `setupComplete=${data.setupComplete} · ${lines.join(' / ')}`;
    }

    async function submitViaRest() {
        const matchSessionId = getMatchSessionId();
        if (!matchSessionId) {
            log('matchSessionId가 비어 있습니다. 「테스트 매치 생성」을 먼저 누르세요.', 'err');
            return null;
        }

        const payload = buildPayload();

        try {
            const res = await fetch(`/game-test/api/match/${matchSessionId}/setup-numbers`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });
            if (!res.ok) {
                const text = await res.text();
                throw new Error(`HTTP ${res.status} — ${text}`);
            }
            const data = await res.json();
            log(`제출 완료 — ${formatRedisResponse(data)}`, 'ok');
            sessionStorage.setItem('bluffball.matchSessionId', matchSessionId);
            return data;
        } catch (e) {
            log(`제출 실패: ${e.message}`, 'err');
            return null;
        }
    }

    async function submitSetupNumbers() {
        if (!isSelectionComplete()) {
            log('모든 숫자를 선택하세요.', 'err');
            return;
        }

        els.btnSubmit.disabled = true;
        const data = await submitViaRest();
        if (data) {
            log('멀리건 화면으로 이동합니다.', 'ok');
            goToMulligan();
            return;
        }
        updateActions();
    }

    async function createTestMatch() {
        try {
            const res = await fetch('/game-test/api/match', { method: 'POST' });
            if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
            }
            const data = await res.json();
            els.matchSessionId.value = data.matchSessionId;
            updateActions();
            log(`테스트 매치 생성 — id=${data.matchSessionId} (userId=${data.pitcherUserId})`, 'ok');
        } catch (e) {
            log(`매치 생성 실패: ${e.message}`, 'err');
        }
    }

    els.btnCreateMatch.addEventListener('click', createTestMatch);
    els.btnConnect.addEventListener('click', connectWebSocket);
    els.btnDisconnect.addEventListener('click', disconnectWebSocket);
    els.btnReset.addEventListener('click', resetSelection);
    els.btnSubmit.addEventListener('click', submitSetupNumbers);
    els.matchSessionId.addEventListener('input', updateActions);
    els.matchSessionId.addEventListener('change', updateActions);

    if (els.pageVersion) {
        els.pageVersion.textContent = `테스트 화면 ${PAGE_VERSION} — 제출 시 멀리건으로 이동`;
    }

    renderPhase();
    setConnected(false);
    updateActions();
    log(`SetupNumber 테스트 화면 ${PAGE_VERSION} — 구버전이면 Ctrl+Shift+R로 새로고침`);
})();
