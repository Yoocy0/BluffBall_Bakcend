(() => {
    const state = {
        pitchHand: [],
        coordinates: [],
        selectedPitchCardId: null,
        selectedCoordinateCardId: null,
        selectedCoordinateNumber: null,
    };

    const els = {
        matchSessionId: document.getElementById('matchSessionId'),
        btnPrepare: document.getElementById('btnPrepare'),
        prepareStatus: document.getElementById('prepareStatus'),
        linkMulligan: document.getElementById('linkMulligan'),
        pitchHand: document.getElementById('pitchHand'),
        coordGrid: document.getElementById('coordGrid'),
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

    function updateSubmitButton() {
        els.btnSubmit.disabled = !getMatchSessionId()
            || !state.selectedPitchCardId
            || !state.selectedCoordinateCardId;
    }

    function renderPitchHand() {
        els.pitchHand.innerHTML = '';
        if (state.pitchHand.length === 0) {
            els.pitchHand.innerHTML = '<p class="phase-desc">구종 카드 없음 — 「게임 준비」를 먼저 실행하세요.</p>';
            return;
        }

        state.pitchHand.forEach((card) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'card-btn';
            if (state.selectedPitchCardId === card.cardId) {
                btn.classList.add('selected');
            }
            btn.innerHTML = `
                <span class="card-name">${card.name}</span>
                <span class="card-meta">변화 ${card.changeAmount} · ${card.direction} · ${card.timing}</span>
            `;
            btn.addEventListener('click', () => {
                state.selectedPitchCardId = card.cardId;
                renderPitchHand();
                updateSubmitButton();
                log(`구종 선택: ${card.name} (id=${card.cardId})`);
            });
            els.pitchHand.appendChild(btn);
        });
    }

    function renderCoordGrid() {
        els.coordGrid.innerHTML = '';
        state.coordinates.forEach((coord) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'coord-btn';
            if (coord.strike) btn.classList.add('strike-zone');
            if (state.selectedCoordinateNumber === coord.coordinateNumber) {
                btn.classList.add('selected');
            }
            btn.textContent = coord.coordinateNumber;
            btn.title = coord.name;
            btn.addEventListener('click', () => {
                state.selectedCoordinateCardId = coord.cardId;
                state.selectedCoordinateNumber = coord.coordinateNumber;
                renderCoordGrid();
                updateSubmitButton();
                log(`좌표 선택: ${coord.coordinateNumber} (id=${coord.cardId})`);
            });
            els.coordGrid.appendChild(btn);
        });
    }

    function applyPrepareResponse(data) {
        state.pitchHand = data.pitchHand || [];
        state.coordinates = data.coordinateOptions || [];
        els.prepareStatus.textContent =
            `준비 완료 — mulligan=${data.mulliganDone}, `
            + `구종 ${state.pitchHand.length}장, 좌표 ${state.coordinates.length}칸`;
        renderPitchHand();
        renderCoordGrid();
        updateSubmitButton();
    }

    async function prepareGame() {
        const matchSessionId = getMatchSessionId();
        if (!matchSessionId) {
            log('matchSessionId를 입력하세요.', 'err');
            return;
        }

        try {
            const res = await fetch(`/game-test/api/match/${matchSessionId}/prepare-pitch`, {
                method: 'POST',
            });
            if (!res.ok) {
                const text = await res.text();
                throw new Error(`HTTP ${res.status} — ${text}`);
            }
            const data = await res.json();
            applyPrepareResponse(data);
            log('게임 준비 완료 (카드 드로우 + 멀리건 확정)', 'ok');
        } catch (e) {
            log(`게임 준비 실패: ${e.message}`, 'err');
        }
    }

    async function submitSelection() {
        const matchSessionId = getMatchSessionId();
        const payload = {
            pitchCardId: state.selectedPitchCardId,
            coordinateCardId: state.selectedCoordinateCardId,
        };

        try {
            const res = await fetch(`/game-test/api/match/${matchSessionId}/pitcher/select-card`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });
            if (!res.ok) {
                const text = await res.text();
                throw new Error(`HTTP ${res.status} — ${text}`);
            }
            const data = await res.json();
            log(`투구 제출 완료 — 시작 ${data.startCoordinateNumber} → 최종 ${data.finalCoordinateNumber}`, 'ok');

            sessionStorage.setItem('bluffball.pitcherResult', JSON.stringify(data));

            const qs = new URLSearchParams({
                matchSessionId,
                start: String(data.startCoordinateNumber),
                final: String(data.finalCoordinateNumber),
                pitch: data.pitchCardName || '',
            });
            window.location.href = `/game-test/PitcherResult.html?${qs.toString()}`;
        } catch (e) {
            log(`투구 제출 실패: ${e.message}`, 'err');
        }
    }

    els.btnPrepare.addEventListener('click', prepareGame);
    els.btnSubmit.addEventListener('click', submitSelection);

    const initialMatchId = readQueryMatchId();
    if (initialMatchId) {
        els.matchSessionId.value = initialMatchId;
        if (els.linkMulligan) {
            els.linkMulligan.href = `/game-test/Mulligan.html?matchSessionId=${encodeURIComponent(initialMatchId)}`;
        }
        log(`matchSessionId=${initialMatchId}`, 'ok');
        prepareGame();
    } else {
        log('URL에 matchSessionId가 없습니다. 직접 입력 후 「게임 준비」를 누르세요.');
    }
})();
