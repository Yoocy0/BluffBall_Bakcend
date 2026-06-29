(() => {
    const state = {
        pitchHand: [],
        mulliganDone: false,
        swapSelectedIds: new Set(),
    };

    const els = {
        matchSessionId: document.getElementById('matchSessionId'),
        mulliganStatus: document.getElementById('mulliganStatus'),
        handStatus: document.getElementById('handStatus'),
        pitchHand: document.getElementById('pitchHand'),
        btnSwap: document.getElementById('btnSwap'),
        btnConfirm: document.getElementById('btnConfirm'),
        btnNext: document.getElementById('btnNext'),
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

        // 멀리건 완료 후에도 교체 버튼은 활성 — 백엔드 거부 응답 테스트용
        els.btnSwap.disabled = !hasMatch || !hasSwapSelection;
        els.btnConfirm.disabled = !hasMatch || done;
        els.btnNext.disabled = !hasMatch || !done;
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
            applyHandResponse(data);
            log(`교체 완료 [${cardIds.join(', ')}] → ${data.pitchHand.map((c) => c.name).join(', ')}`, 'ok');
        } catch (e) {
            log(`교체 실패: ${e.message}`, 'err');
        }
    }

    async function confirmHand() {
        const matchSessionId = getMatchSessionId();

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
            log(`확정 완료 — ${data.pitchHand.map((c) => c.name).join(', ')}`, 'ok');
        } catch (e) {
            log(`확정 실패: ${e.message}`, 'err');
        }
    }

    function goToPitcherSelect() {
        const matchSessionId = getMatchSessionId();
        if (!state.mulliganDone) {
            log('멀리건을 먼저 완료하세요.', 'err');
            return;
        }
        sessionStorage.setItem('bluffball.matchSessionId', matchSessionId);
        window.location.href = `/game-test/PitcherSelect.html?matchSessionId=${encodeURIComponent(matchSessionId)}`;
    }

    els.btnSwap.addEventListener('click', swapCards);
    els.btnConfirm.addEventListener('click', confirmHand);
    els.btnNext.addEventListener('click', goToPitcherSelect);

    const initialMatchId = readQueryMatchId();
    if (initialMatchId) {
        els.matchSessionId.value = initialMatchId;
        log(`matchSessionId=${initialMatchId}`, 'ok');
        prepareDraw();
    } else {
        log('URL에 matchSessionId가 없습니다.');
    }
})();
