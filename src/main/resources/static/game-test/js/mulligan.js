(() => {
    const NAVIGATE_DELAY_MS = 1600;
    const { getMatchSessionId, goWithMatch } = window.BluffBallNav;

    const state = {
        pitchHand: [],
        mulliganDone: false,
        swapSelectedIds: new Set(),
        navigating: false,
        awaitingMulliganResponse: false,
        pendingSwapBefore: null,
        pendingSwapIds: [],
    };

    const els = {
        mulliganStatus: document.getElementById('mulliganStatus'),
        handStatus: document.getElementById('handStatus'),
        pitchHand: document.getElementById('pitchHand'),
        swapResultPanel: document.getElementById('swapResultPanel'),
        swapResultList: document.getElementById('swapResultList'),
        waitPanel: document.getElementById('mulliganWaitPanel'),
        btnSwap: document.getElementById('btnSwap'),
        btnConfirm: document.getElementById('btnConfirm'),
        log: document.getElementById('log'),
    };

    function log(message, type = '') {
        const line = document.createElement('div');
        if (type) line.className = type;
        line.textContent = `[${new Date().toLocaleTimeString()}] ${message}`;
        els.log.prepend(line);
    }

    function formatCard(card) {
        if (!card) {
            return { name: '?', meta: '-' };
        }
        return {
            name: card.name,
            meta: `변화 ${card.changeAmount} · ${card.direction} · ${card.timing}`,
        };
    }

    function renderCardBlock(card, toneClass) {
        const { name, meta } = formatCard(card);
        return `
            <div class="${toneClass}">
                <span class="swap-card-name">${name}</span>
                <span class="swap-card-meta">${meta}</span>
            </div>
        `;
    }

    function showSwapResults(pairs) {
        if (!els.swapResultPanel || !els.swapResultList) {
            return;
        }
        els.swapResultList.innerHTML = '';
        pairs.forEach(({ before, after }) => {
            const row = document.createElement('div');
            row.className = 'swap-result-row';
            row.innerHTML =
                renderCardBlock(before, 'swap-before')
                + '<div class="swap-arrow">→</div>'
                + renderCardBlock(after, 'swap-after');
            els.swapResultList.appendChild(row);
        });
        els.swapResultPanel.hidden = pairs.length === 0;
    }

    function hideSwapResults() {
        if (els.swapResultPanel) {
            els.swapResultPanel.hidden = true;
        }
        if (els.swapResultList) {
            els.swapResultList.innerHTML = '';
        }
    }

    function showWaitForOpponent() {
        if (els.waitPanel) {
            els.waitPanel.hidden = false;
        }
        els.mulliganStatus.textContent = '상대 멀리건 대기';
        els.mulliganStatus.className = 'status disconnected';
    }

    function setMulliganStatus(done) {
        state.mulliganDone = done;
        if (done && !BluffBallGameWs.isAllMulliganReady()) {
            showWaitForOpponent();
            return;
        }
        els.mulliganStatus.textContent = done ? '멀리건 완료' : '멀리건 가능';
        els.mulliganStatus.className = `status ${done ? 'connected' : 'disconnected'}`;
        updateButtons();
    }

    function updateButtons() {
        const hasMatch = !!getMatchSessionId();
        const hasSwapSelection = state.swapSelectedIds.size > 0;
        const done = state.mulliganDone;
        const busy = state.navigating || state.awaitingMulliganResponse;

        els.btnSwap.disabled = busy || !hasMatch || !BluffBallGameWs.isConnected() || !hasSwapSelection || done;
        els.btnConfirm.disabled = busy || !hasMatch || !BluffBallGameWs.isConnected() || done;
    }

    function renderPitchHand() {
        els.pitchHand.innerHTML = '';
        if (state.pitchHand.length === 0) {
            els.pitchHand.innerHTML = '<p class="phase-desc">패가 비어 있습니다. setup 완료 후 CardHandEvent를 기다리세요.</p>';
            return;
        }

        state.pitchHand.forEach((card) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'card-btn';
            if (state.swapSelectedIds.has(card.cardId)) {
                btn.classList.add('swap-selected');
            }
            btn.innerHTML = `
                <span class="card-name">${card.name}</span>
                <span class="card-meta">변화 ${card.changeAmount} · ${card.direction} · ${card.timing}</span>
            `;
            btn.addEventListener('click', () => {
                if (state.mulliganDone || state.navigating || state.awaitingMulliganResponse) {
                    return;
                }
                if (state.swapSelectedIds.has(card.cardId)) {
                    state.swapSelectedIds.delete(card.cardId);
                } else {
                    state.swapSelectedIds.add(card.cardId);
                }
                renderPitchHand();
                updateButtons();
            });
            els.pitchHand.appendChild(btn);
        });
    }

    function applyHand(cards) {
        state.pitchHand = cards || [];
        state.swapSelectedIds.clear();
        renderPitchHand();
        els.handStatus.textContent = `구종 ${state.pitchHand.length}장`;
    }

    function buildSwapPairs(beforeHand, swapIds, afterHand) {
        const swapIdSet = new Set(swapIds);
        const keepIdSet = new Set(
            beforeHand.filter((card) => !swapIdSet.has(card.cardId)).map((card) => card.cardId),
        );
        const beforeSwapped = beforeHand.filter((card) => swapIdSet.has(card.cardId));
        const afterNew = afterHand.filter((card) => !keepIdSet.has(card.cardId));

        return beforeSwapped.map((before, index) => ({
            before,
            after: afterNew[index] || null,
        }));
    }

    function goToNextPhase(delayMs = 0) {
        const matchSessionId = getMatchSessionId();
        if (!matchSessionId) {
            log('matchSessionId가 없습니다.', 'err');
            state.navigating = false;
            state.awaitingMulliganResponse = false;
            updateButtons();
            return;
        }

        if (!BluffBallGameWs.isAllMulliganReady()) {
            showWaitForOpponent();
            state.navigating = false;
            state.awaitingMulliganResponse = false;
            updateButtons();
            return;
        }

        const path = BluffBallRole.isPitcher()
            ? '/game-test/PitcherSelect.html'
            : '/game-test/BatterWait.html';

        const navigate = () => goWithMatch(path, matchSessionId);

        if (delayMs > 0) {
            log(`${delayMs / 1000}초 후 다음 화면으로 이동합니다.`, 'ok');
            setTimeout(navigate, delayMs);
            return;
        }
        navigate();
    }

    function publishMulligan(cardIdsToSwap) {
        if (!BluffBallGameWs.isConnected()) {
            log('WebSocket 연결 후 진행하세요.', 'err');
            return;
        }

        state.awaitingMulliganResponse = true;
        state.navigating = true;
        updateButtons();

        try {
            BluffBallGameWs.publish('cards/mulligan', { cardIdsToSwap });
            log(`cards/mulligan 전송 — 교체 ${cardIdsToSwap.length}장`, 'ok');
        } catch (e) {
            state.awaitingMulliganResponse = false;
            state.navigating = false;
            updateButtons();
            log(`멀리건 전송 실패: ${e.message}`, 'err');
        }
    }

    function swapCards() {
        const cardIds = [...state.swapSelectedIds];
        state.pendingSwapBefore = [...state.pitchHand];
        state.pendingSwapIds = cardIds;
        hideSwapResults();
        publishMulligan(cardIds);
    }

    function confirmHand() {
        hideSwapResults();
        state.pendingSwapBefore = null;
        state.pendingSwapIds = [];
        publishMulligan([]);
    }

    function handleCardHandEvent({ event }) {
        if (!BluffBallGameWs.isCardHandForMe(event)) {
            return;
        }

        if (event?.pitcherUserId != null) {
            BluffBallRole.syncRoleFromPitcherUserId(event.pitcherUserId);
        }

        applyHand(event.cards);
        log(`CardHandEvent — ${(event.cards || []).map((c) => c.name).join(', ')}`, 'ok');

        if (event.fromMulligan !== true && !BluffBallGameWs.isAllMulliganReady()) {
            state.mulliganDone = false;
            setMulliganStatus(false);
            return;
        }

        if (state.awaitingMulliganResponse) {
            if (state.pendingSwapBefore && state.pendingSwapIds?.length) {
                const pairs = buildSwapPairs(state.pendingSwapBefore, state.pendingSwapIds, event.cards || []);
                showSwapResults(pairs);
                pairs.forEach(({ before, after }) => {
                    log(`교체: ${before.name} → ${after ? after.name : '?'}`, 'ok');
                });
            } else {
                log(`교체 없이 확정 — ${(event.cards || []).map((c) => c.name).join(', ')}`, 'ok');
            }
            state.awaitingMulliganResponse = false;
        }

        setMulliganStatus(true);

        if (event.allMulliganReady) {
            goToNextPhase(NAVIGATE_DELAY_MS);
        } else {
            state.navigating = false;
            showWaitForOpponent();
            updateButtons();
        }
    }

    function connectWebSocket(matchSessionId) {
        BluffBallGameWs.on(BluffBallGameWs.EVENT.CARD_HAND, handleCardHandEvent);

        BluffBallGameWs.connect({
            matchSessionId,
            reconnectDelay: 5000,
            onConnect: () => {
                log('WebSocket 연결됨', 'ok');
                updateButtons();
            },
            onError: (frame) => {
                log(`STOMP 오류: ${frame.headers['message'] || frame.body}`, 'err');
                updateButtons();
            },
            onDisconnect: () => {
                log('WebSocket 연결 종료', 'err');
                updateButtons();
            },
        });
    }

    function init() {
        if (!BluffBallRole.requireLoginOrRedirect()) {
            return;
        }

        BluffBallRole.ensureRoleSyncedFromStorage();

        const matchSessionId = getMatchSessionId();
        if (!matchSessionId) {
            goWithMatch('/game-test/Home.html');
            return;
        }

        if (BluffBallGameWs.isAllMulliganReady()) {
            goToNextPhase(0);
            return;
        }

        const storedHand = BluffBallGameWs.getStoredPitchHand();
        if (storedHand?.length) {
            applyHand(storedHand);
            log('저장된 카드 패 로드', 'ok');
        } else {
            log('카드 패 없음 — CardHandEvent 대기', 'err');
        }

        if (BluffBallGameWs.isMyMulliganDone()) {
            setMulliganStatus(true);
        } else {
            setMulliganStatus(false);
        }

        els.btnSwap.addEventListener('click', swapCards);
        els.btnConfirm.addEventListener('click', confirmHand);
        connectWebSocket(matchSessionId);
    }

    init();
})();
