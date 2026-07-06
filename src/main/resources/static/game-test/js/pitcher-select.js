(() => {
    const { getMatchSessionId, goWithMatch, mountInGameHud } = window.BluffBallNav;

    const pitchHand = document.getElementById('pitchHand');

    function renderPitchHand(cards) {
        pitchHand.innerHTML = '';
        if (!cards?.length) {
            pitchHand.innerHTML = '<p class="phase-desc">구종 패 없음 — 멀리건을 먼저 완료하세요.</p>';
            return;
        }

        cards.forEach((card) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'card-btn';
            btn.innerHTML = `
                <span class="card-name">${card.name}</span>
                <span class="card-meta">변화 ${card.changeAmount} · ${card.direction} · ${card.timing}</span>
            `;
            btn.addEventListener('click', () => {
                sessionStorage.setItem('bluffball.selectedPitchCard', JSON.stringify(card));
                goWithMatch('/game-test/PitcherCoordSelect.html');
            });
            pitchHand.appendChild(btn);
        });
    }

    function handleTurnResult() {
        goWithMatch('/game-test/BatterResult.html');
    }

    function init() {
        if (!BluffBallRole.requireLoginOrRedirect()) {
            return;
        }

        BluffBallRole.ensureRoleSyncedFromStorage();

        if (!BluffBallRole.isPitcher()) {
            goWithMatch('/game-test/BatterWait.html');
            return;
        }

        const matchSessionId = getMatchSessionId();
        if (!matchSessionId || !BluffBallGameWs.isAllMulliganReady()) {
            goWithMatch('/game-test/Mulligan.html');
            return;
        }

        const cards = BluffBallGameWs.getStoredPitchHand();
        renderPitchHand(cards);

        BluffBallGameWs.attachInGamePhaseGuard();
        BluffBallGameWs.on(BluffBallGameWs.EVENT.TURN_RESULT, handleTurnResult);

        BluffBallGameWs.connect({
            matchSessionId,
            reconnectDelay: 5000,
        });

        mountInGameHud();
    }

    init();
})();
