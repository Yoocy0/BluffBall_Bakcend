(() => {
    const NAVIGATE_DELAY_MS = 1600;

    const state = {
        pitchHand: [],
        mulliganDone: false,
        swapSelectedIds: new Set(),
        navigating: false,
    };

    const els = {
        matchSessionId: document.getElementById('matchSessionId'),
        mulliganStatus: document.getElementById('mulliganStatus'),
        handStatus: document.getElementById('handStatus'),
        pitchHand: document.getElementById('pitchHand'),
        swapResultPanel: document.getElementById('swapResultPanel'),
        swapResultList: document.getElementById('swapResultList'),
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

    function getMatchSessionId() {
        return els.matchSessionId.value.trim();
    }

    function readQueryMatchId() {
        const params = new URLSearchParams(window.location.search);
        return params.get('matchSessionId') || sessionStorage.getItem('bluffball.matchSessionId') || '';
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

    function setMulliganStatus(done) {
        state.mulliganDone = done;
        els.mulliganStatus.textContent = done ? '멀리건 완료' : '멀리건 가능';
        els.mulliganStatus.className = `status ${done ? 'connected' : 'disconnected'}`;
        updateButtons();
    }

    function updateButtons() {
        const hasMatch = !!getMatchSessionId();
        const hasSwapSelection = state.swapSelectedIds.size > 0;
        const done = state.mulliganDone;
        const busy = state.navigating;

        els.btnSwap.disabled = busy || !hasMatch || !hasSwapSelection || done;
        els.btnConfirm.disabled = busy || !hasMatch || done;
    }

    function renderPitchHand() {
        els.pitchHand.innerHTML = '';
        if (state.pitchHand.length === 0) {
            els.pitchHand.innerHTML = '<p class="phase-desc">패가 비어 있습니다.</p>';
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
                if (state.mulliganDone || state.navigating) {
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

    function applyHandResponse(data) {
        state.pitchHand = data.pitchHand || [];
        state.swapSelectedIds.clear();
        setMulliganStatus(data.mulliganDone);
        els.handStatus.textContent =
            `구종 ${state.pitchHand.length}장 · setup=${data.setupComplete} · mulligan=${data.mulliganDone}`;
        renderPitchHand();
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

    function goToPitcherSelect(delayMs = 0) {
        const matchSessionId = getMatchSessionId();
        if (!matchSessionId) {
            log('matchSessionId가 없습니다.', 'err');
            state.navigating = false;
            updateButtons();
            return;
        }

        const navigate = () => {
            sessionStorage.setItem('bluffball.matchSessionId', matchSessionId);
            window.location.href =
                `/game-test/PitcherSelect.html?matchSessionId=${encodeURIComponent(matchSessionId)}`;
        };

        if (delayMs > 0) {
            log(`${delayMs / 1000}초 후 투수 선택 화면으로 이동합니다.`, 'ok');
            setTimeout(navigate, delayMs);
            return;
        }
        navigate();
    }

    async function prepareDraw() {
        const matchSessionId = getMatchSessionId();
        if (!matchSessionId) {
            log('matchSessionId를 입력하세요.', 'err');
            return;
        }

        try {
            const res = await fetch(`/game-test/api/match/${matchSessionId}/prepare-draw`, {
                method: 'POST',
            });
            if (!res.ok) {
                const text = await res.text();
                throw new Error(`HTTP ${res.status} — ${text}`);
            }
            const data = await res.json();
            applyHandResponse(data);
            log(`카드 드로우 완료 — ${data.pitchHand.map((c) => c.name).join(', ')}`, 'ok');
        } catch (e) {
            log(`드로우 실패: ${e.message}`, 'err');
        }
    }

    async function swapCards() {
        const matchSessionId = getMatchSessionId();
        const cardIds = [...state.swapSelectedIds];
        const beforeHand = [...state.pitchHand];

        state.navigating = true;
        updateButtons();

        try {
            const res = await fetch(`/game-test/api/match/${matchSessionId}/mulligan/swap`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ cardIdsToSwap: cardIds }),
            });
            if (!res.ok) {
                const text = await res.text();
                throw new Error(`HTTP ${res.status} — ${text}`);
            }
            const data = await res.json();
            const pairs = buildSwapPairs(beforeHand, cardIds, data.pitchHand || []);

            showSwapResults(pairs);
            applyHandResponse(data);

            pairs.forEach(({ before, after }) => {
                const afterName = after ? after.name : '?';
                log(`교체: ${before.name} → ${afterName}`, 'ok');
            });

            goToPitcherSelect(NAVIGATE_DELAY_MS);
        } catch (e) {
            state.navigating = false;
            updateButtons();
            log(`교체 실패: ${e.message}`, 'err');
        }
    }

    async function confirmHand() {
        const matchSessionId = getMatchSessionId();

        state.navigating = true;
        hideSwapResults();
        updateButtons();

        try {
            const res = await fetch(`/game-test/api/match/${matchSessionId}/mulligan/confirm`, {
                method: 'POST',
            });
            if (!res.ok) {
                const text = await res.text();
                throw new Error(`HTTP ${res.status} — ${text}`);
            }
            const data = await res.json();
            applyHandResponse(data);
            log(`교체 없이 확정 — ${data.pitchHand.map((c) => c.name).join(', ')}`, 'ok');
            goToPitcherSelect(NAVIGATE_DELAY_MS);
        } catch (e) {
            state.navigating = false;
            updateButtons();
            log(`확정 실패: ${e.message}`, 'err');
        }
    }

    els.btnSwap.addEventListener('click', swapCards);
    els.btnConfirm.addEventListener('click', confirmHand);

    const initialMatchId = readQueryMatchId();
    if (initialMatchId) {
        els.matchSessionId.value = initialMatchId;
        log(`matchSessionId=${initialMatchId}`, 'ok');
        prepareDraw();
    } else {
        log('URL에 matchSessionId가 없습니다.');
    }
})();
