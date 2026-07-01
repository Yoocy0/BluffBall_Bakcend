(() => {
    const { getMatchSessionId, goWithMatch, mountBroadcastHud } = window.BluffBallNav;

    const statusEl = document.getElementById('batterWaitStatus');

    function setStatus(text) {
        if (statusEl) {
            statusEl.textContent = text;
        }
    }

    function goToBatterPlay(startCoordinateNumber) {
        sessionStorage.setItem('bluffball.startCoordinate', String(startCoordinateNumber));
        sessionStorage.removeItem('bluffball.batterCoordinate');
        sessionStorage.removeItem('bluffball.batterTimerStartedAt');
        goWithMatch('/game-test/BatterCoordSelect.html');
    }

    function handlePitcherReady({ event }) {
        setStatus('투수 준비 완료 — 타격 화면으로 이동합니다.');
        goToBatterPlay(event.startCoordinateNumber);
    }

    function handleGameEnd() {
        goWithMatch('/game-test/GameEnd.html');
    }

    function init() {
        if (!BluffBallRole.requireLoginOrRedirect()) {
            return;
        }

        BluffBallRole.ensureRoleSyncedFromStorage();

        if (!BluffBallRole.isBatter()) {
            if (BluffBallRole.isPitcher()) {
                goWithMatch('/game-test/PitcherSelect.html');
            } else if (!BluffBallGameWs.isAllMulliganReady()) {
                goWithMatch('/game-test/Mulligan.html');
            } else {
                goWithMatch('/game-test/SetupNumber.html');
            }
            return;
        }

        const matchSessionId = getMatchSessionId();
        if (!matchSessionId) {
            goWithMatch('/game-test/Home.html');
            return;
        }

        if (!BluffBallGameWs.isAllMulliganReady()) {
            goWithMatch('/game-test/Mulligan.html');
            return;
        }

        sessionStorage.removeItem('bluffball.startCoordinate');
        sessionStorage.removeItem('bluffball.batterCoordinate');
        sessionStorage.removeItem('bluffball.batterTimerStartedAt');

        BluffBallGameWs.attachInGamePhaseGuard();
        BluffBallGameWs.on(BluffBallGameWs.EVENT.PITCHER_READY, handlePitcherReady);
        BluffBallGameWs.on(BluffBallGameWs.EVENT.GAME_END, handleGameEnd);

        BluffBallGameWs.connect({
            matchSessionId,
            reconnectDelay: 5000,
            onConnect: () => setStatus('투수 선택을 기다리는 중…'),
            onError: () => setStatus('연결 오류 — 새로고침 후 다시 시도하세요.'),
            onDisconnect: () => setStatus('연결이 끊어졌습니다.'),
        });

        mountBroadcastHud();
    }

    init();
})();
