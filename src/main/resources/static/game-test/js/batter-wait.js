(() => {
    const { getMatchSessionId, goWithMatch, mountInGameHud } = window.BluffBallNav;

    const statusEl = document.getElementById('batterWaitStatus');
    let pollTimer = null;

    function setStatus(text) {
        if (statusEl) {
            statusEl.textContent = text;
        }
    }

    function stopPoll() {
        if (pollTimer) {
            clearInterval(pollTimer);
            pollTimer = null;
        }
    }

    function goToBatterPlay(startCoordinateNumber) {
        stopPoll();
        sessionStorage.setItem('bluffball.startCoordinate', String(startCoordinateNumber));
        sessionStorage.removeItem('bluffball.batterCoordinate');
        sessionStorage.removeItem('bluffball.batterTimerStartedAt');
        goWithMatch('/game-test/BatterCoordSelect.html');
    }

    function handlePitcherReady({ event }) {
        setStatus('투수 준비 완료 — 타격 화면으로 이동합니다.');
        BluffBallNav.setTurnBanner({
            tone: 'mine',
            title: '내 차례 — 타격하세요',
            sub: '투수가 준비됐습니다. 타격 화면으로 이동합니다.',
            connected: true,
        });
        goToBatterPlay(event.startCoordinateNumber);
    }

    function handleGameEnd() {
        stopPoll();
        goWithMatch('/game-test/GameEnd.html');
    }

    /**
     * WS PitcherReady를 놓친 경우 세션 상태로 따라잡는다.
     * (봇이 화면 전환보다 먼저 투구 선택하는 레이스 대응)
     */
    async function catchUpIfPitcherAlreadyReady(matchSessionId) {
        const session = await BluffBallNav.fetchSessionState(matchSessionId);
        if (!session) {
            return false;
        }
        BluffBallNav.applySessionState(session);

        if (BluffBallRole.isPitcher()) {
            stopPoll();
            goWithMatch('/game-test/PitcherSelect.html');
            return true;
        }

        const turn = session.turn;
        if (turn?.pitcherSelectionComplete && turn.startCoordinateNumber > 0) {
            setStatus('투수 준비 완료(복구) — 타격 화면으로 이동합니다.');
            goToBatterPlay(turn.startCoordinateNumber);
            return true;
        }
        return false;
    }

    async function loadPitchTips() {
        const rail = document.getElementById('pitchTipsRail');
        const listEl = document.getElementById('pitchTipsList');
        if (!rail || !listEl) {
            return;
        }

        try {
            const cards = await BluffBallCards.fetchPitchCards();
            BluffBallCards.mountPitchTipsRail(listEl, cards);
            rail.hidden = false;
        } catch (e) {
            setStatus(`구종 정보를 불러오지 못했습니다. (${e.message || e})`);
        }
    }

    function startWaiting(matchSessionId) {
        sessionStorage.removeItem('bluffball.startCoordinate');
        sessionStorage.removeItem('bluffball.batterCoordinate');
        sessionStorage.removeItem('bluffball.batterTimerStartedAt');

        BluffBallGameWs.attachInGamePhaseGuard();
        BluffBallGameWs.on(BluffBallGameWs.EVENT.PITCHER_READY, handlePitcherReady);
        BluffBallGameWs.on(BluffBallGameWs.EVENT.GAME_END, handleGameEnd);

        BluffBallGameWs.connect({
            matchSessionId,
            reconnectDelay: 1500,
            onConnect: () => {
                setStatus('투수 선택을 기다리는 중…');
                BluffBallNav.setTurnBanner({
                    tone: 'wait',
                    title: '대기 중 — 투수(봇)가 구종을 고르는 중',
                    sub: '봇은 약 4~5초 간격으로 행동합니다.',
                    connected: true,
                });
                catchUpIfPitcherAlreadyReady(matchSessionId).catch(() => { /* ignore */ });
            },
            onError: () => {
                setStatus('연결 오류 — 재연결을 시도합니다.');
                BluffBallNav.setTurnBanner({
                    tone: 'wait',
                    title: '연결 끊김 — 재연결 중',
                    connected: false,
                });
            },
            onDisconnect: () => {
                setStatus('연결이 끊어졌습니다. 재연결 중…');
                BluffBallNav.setTurnBanner({
                    tone: 'wait',
                    title: '연결 끊김 — 재연결 중',
                    connected: false,
                });
            },
        });

        // WS 이벤트를 놓쳐도 주기적으로 복구 + 턴 배너 갱신
        pollTimer = setInterval(() => {
            catchUpIfPitcherAlreadyReady(matchSessionId).catch(() => { /* ignore */ });
            BluffBallNav.pollTurnBanner(matchSessionId, () => BluffBallGameWs.isConnected())
                .catch(() => { /* ignore */ });
        }, 1000);

        mountInGameHud();
        loadPitchTips();
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

        if (!BluffBallRole.getRole() || !BluffBallNav.getGameMode()) {
            const session = await BluffBallNav.fetchSessionState(matchSessionId);
            if (session) {
                BluffBallNav.applySessionState(session);
            }
        }

        if (BluffBallRole.isPitcher()) {
            goWithMatch('/game-test/PitcherSelect.html');
            return;
        }

        if (!BluffBallRole.isBatter()) {
            BluffBallRole.setRole('batter');
        }

        if (!BluffBallGameWs.isAllMulliganReady() && BluffBallNav.usesInGameMulligan()) {
            goWithMatch('/game-test/Mulligan.html');
            return;
        }

        // 이미 투수가 던진 상태면 바로 타격으로
        const caughtUp = await catchUpIfPitcherAlreadyReady(matchSessionId);
        if (caughtUp) {
            return;
        }

        // 내 타석이 아니면 관전 화면으로
        const session = await BluffBallNav.fetchSessionState(matchSessionId);
        if (session) {
            BluffBallNav.applySessionState(session);
            const decision = BluffBallNav.routeBySession(session);
            if (decision?.page && decision.page !== '/game-test/BatterWait.html') {
                goWithMatch(decision.page);
                return;
            }
        }

        BluffBallNav.setTurnBanner({
            tone: 'wait',
            title: '대기 중 — 투수가 구종을 고르는 중',
            sub: '내 타석입니다. 투수가 준비되면 타격 화면으로 이동합니다.',
            connected: null,
        });

        startWaiting(matchSessionId);
    }

    window.addEventListener('beforeunload', stopPoll);

    init().catch((e) => {
        setStatus(e.message || String(e));
    });
})();
