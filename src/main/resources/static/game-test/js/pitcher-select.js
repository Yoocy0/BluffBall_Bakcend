(() => {
    const { getMatchSessionId, goWithMatch, mountBroadcastHud } = window.BluffBallNav;

    const state = { pitchHand: [] };
    const pitchHand = document.getElementById('pitchHand');

    async function prepareGame() {
        const matchSessionId = getMatchSessionId();
        if (!matchSessionId) {
            goWithMatch('/game-test/Home.html');
            return;
        }

        const res = await fetch(`/game-test/api/match/${matchSessionId}/prepare-pitch`, {
            method: 'POST',
        });
        if (!res.ok) {
            throw new Error(`HTTP ${res.status}`);
        }
        const data = await res.json();
        state.pitchHand = data.pitchHand || [];
        renderPitchHand();
    }

    function renderPitchHand() {
        pitchHand.innerHTML = '';
        if (state.pitchHand.length === 0) {
            pitchHand.innerHTML = '<p class="phase-desc">구종 패 없음</p>';
            return;
        }

        state.pitchHand.forEach((card) => {
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

    mountBroadcastHud();
    prepareGame().catch(() => goWithMatch('/game-test/Home.html'));
})();
