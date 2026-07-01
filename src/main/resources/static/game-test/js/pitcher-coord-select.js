(() => {
    const { getMatchSessionId, goWithMatch, mountBroadcastHud } = window.BluffBallNav;

    const selectedPitchSummary = document.getElementById('selectedPitchSummary');
    const coordGrid = document.getElementById('coordGrid');
    const waitPanel = document.getElementById('waitPanel');

    function readSelectedPitchCard() {
        try {
            return JSON.parse(sessionStorage.getItem('bluffball.selectedPitchCard') || 'null');
        } catch (_) {
            return null;
        }
    }

    function renderSelectedPitch(card) {
        if (!card) {
            selectedPitchSummary.innerHTML = '<p class="phase-desc">구종 없음</p>';
            return;
        }
        selectedPitchSummary.innerHTML = `
            <p class="pitch-selected-name">${card.name}</p>
            <p class="pitch-selected-meta">변화 ${card.changeAmount} · ${card.direction} · ${card.timing}</p>
        `;
    }

    function showWaitingForBatter() {
        if (waitPanel) {
            waitPanel.hidden = false;
        }
        coordGrid.querySelectorAll('button').forEach((btn) => { btn.disabled = true; });
    }

    function submitPitch(coordinateCardId) {
        const pitchCard = readSelectedPitchCard();
        if (!pitchCard?.cardId) {
            goWithMatch('/game-test/PitcherSelect.html');
            return;
        }

        if (!BluffBallGameWs.isConnected()) {
            return;
        }

        BluffBallGameWs.publish('pitcher/select-card', {
            pitchCardId: pitchCard.cardId,
            coordinateCardId,
        });

        sessionStorage.removeItem('bluffball.batterCoordinate');
        sessionStorage.removeItem('bluffball.batterTimerStartedAt');
        showWaitingForBatter();
    }

    function renderCoordGrid(coordinates) {
        coordGrid.innerHTML = '';
        coordinates.forEach((coord) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'coord-btn';
            if (coord.strike) btn.classList.add('strike-zone');
            btn.textContent = coord.coordinateNumber;
            btn.addEventListener('click', () => submitPitch(coord.cardId));
            coordGrid.appendChild(btn);
        });
    }

    function handleTurnResult() {
        goWithMatch('/game-test/BatterResult.html');
    }

    async function init() {
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

        const pitchCard = readSelectedPitchCard();
        if (!pitchCard) {
            goWithMatch('/game-test/PitcherSelect.html');
            return;
        }

        renderSelectedPitch(pitchCard);

        BluffBallGameWs.attachInGamePhaseGuard();
        BluffBallGameWs.on(BluffBallGameWs.EVENT.TURN_RESULT, handleTurnResult);

        BluffBallGameWs.connect({
            matchSessionId,
            reconnectDelay: 5000,
        });

        try {
            const coordinates = await BluffBallCards.fetchPitcherCoordinateOptions();
            renderCoordGrid(coordinates);
        } catch (_) {
            goWithMatch('/game-test/PitcherSelect.html');
            return;
        }

        mountBroadcastHud();
    }

    init();
})();
