(() => {
    const { getMatchSessionId, goWithMatch, mountBroadcastHud } = window.BluffBallNav;

    const selectedPitchSummary = document.getElementById('selectedPitchSummary');
    const coordGrid = document.getElementById('coordGrid');

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

    async function submitPitch(coordinateCardId) {
        const matchSessionId = getMatchSessionId();
        const pitchCard = readSelectedPitchCard();
        if (!pitchCard?.cardId) {
            goWithMatch('/game-test/PitcherSelect.html');
            return;
        }

        const res = await fetch(`/game-test/api/match/${matchSessionId}/pitcher/select-card`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                pitchCardId: pitchCard.cardId,
                coordinateCardId,
            }),
        });
        if (!res.ok) {
            throw new Error(`HTTP ${res.status}`);
        }
        const data = await res.json();
        sessionStorage.setItem('bluffball.startCoordinate', String(data.startCoordinateNumber));
        sessionStorage.removeItem('bluffball.batterCoordinate');
        sessionStorage.removeItem('bluffball.batterTimerStartedAt');
        goWithMatch('/game-test/BatterCoordSelect.html');
    }

    async function loadCoordinates() {
        const matchSessionId = getMatchSessionId();
        const res = await fetch(`/game-test/api/match/${matchSessionId}/prepare-pitch`, {
            method: 'POST',
        });
        if (!res.ok) {
            throw new Error(`HTTP ${res.status}`);
        }
        const data = await res.json();
        renderCoordGrid(data.coordinateOptions || []);
    }

    function renderCoordGrid(coordinates) {
        coordGrid.innerHTML = '';
        coordinates.forEach((coord) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'coord-btn';
            if (coord.strike) btn.classList.add('strike-zone');
            btn.textContent = coord.coordinateNumber;
            btn.addEventListener('click', () => {
                submitPitch(coord.cardId).catch(() => goWithMatch('/game-test/PitcherSelect.html'));
            });
            coordGrid.appendChild(btn);
        });
    }

    const pitchCard = readSelectedPitchCard();
    renderSelectedPitch(pitchCard);
    mountBroadcastHud();

    if (!pitchCard || !getMatchSessionId()) {
        goWithMatch('/game-test/PitcherSelect.html');
    } else {
        loadCoordinates().catch(() => goWithMatch('/game-test/PitcherSelect.html'));
    }
})();
