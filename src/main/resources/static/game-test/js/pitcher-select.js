(() => {
    const { getMatchSessionId, goWithMatch, mountInGameHud } = window.BluffBallNav;

    const pitchHand = document.getElementById('pitchHand');

    function renderPitchHand(cards) {
        pitchHand.innerHTML = '';
        if (!cards?.length) {
            const hint = BluffBallNav.isLeagueMode()
                ? '구종 패를 불러오는 중…'
                : '구종 패 없음 — 멀리건을 먼저 완료하세요.';
            pitchHand.innerHTML = `<p class="phase-desc">${hint}</p>`;
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

    async function init() {
        if (!BluffBallRole.requireLoginOrRedirect()) {
            return;
        }

        const matchSessionId = getMatchSessionId();
        if (!matchSessionId) {
            goWithMatch('/game-test/Home.html');
            return;
        }

        BluffBallRole.ensureRoleSyncedFromStorage();

        const session = await BluffBallNav.fetchSessionState(matchSessionId);
        if (session) {
            BluffBallNav.applySessionState(session);
            const decision = BluffBallNav.routeBySession(session);
            if (decision?.page && decision.page !== '/game-test/PitcherSelect.html') {
                goWithMatch(decision.page);
                return;
            }
        }

        if (!BluffBallRole.isPitcher()) {
            goWithMatch('/game-test/BotSpectate.html');
            return;
        }

        if (!BluffBallGameWs.isAllMulliganReady() && BluffBallNav.usesInGameMulligan()) {
            goWithMatch('/game-test/Mulligan.html');
            return;
        }

        renderPitchHand(BluffBallGameWs.getStoredPitchHand());

        BluffBallNav.setTurnBanner({
            tone: 'mine',
            title: '내 차례 — 투수: 구종을 선택하세요',
            sub: '카드를 누르면 좌표 선택으로 이동합니다.',
            connected: null,
        });

        BluffBallGameWs.attachInGamePhaseGuard();
        BluffBallGameWs.on(BluffBallGameWs.EVENT.TURN_RESULT, handleTurnResult);

        BluffBallGameWs.connect({
            matchSessionId,
            reconnectDelay: 1500,
            onConnect: () => {
                BluffBallNav.pollTurnBanner(matchSessionId, () => true).catch(() => {});
            },
            onDisconnect: () => {
                BluffBallNav.setTurnBanner({
                    tone: 'mine',
                    title: '내 차례 — 투수 (연결 재시도 중)',
                    connected: false,
                });
            },
        });

        mountInGameHud();
        setInterval(() => {
            BluffBallNav.pollTurnBanner(matchSessionId, () => BluffBallGameWs.isConnected())
                .catch(() => {});
        }, 2000);
    }

    init().catch(() => {
        goWithMatch('/game-test/BatterWait.html');
    });
})();
