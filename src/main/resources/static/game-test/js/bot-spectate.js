(() => {
    const {
        getMatchSessionId,
        goWithMatch,
        mountInGameHud,
        setTurnBanner,
        pollTurnBanner,
        describeActor,
        routeBySession,
        fetchSessionState,
        applySessionState,
        createConnectionTracker,
        renderBroadcastHud,
    } = window.BluffBallNav;

    const STRIKE_ZONE = new Set([7, 8, 9, 12, 13, 14, 17, 18, 19]);

    const RESULT_LABEL = {
        STRIKE: '스트라이크',
        BALL: '볼',
        SINGLE: '1루타',
        DOUBLE: '2루타',
        TRIPLE: '3루타',
        HOMERUN: '홈런',
        OUT: '아웃',
        DOUBLE_PLAY: '병살',
        WILD_PITCH: '폭투',
        WALK: '볼넷',
        STRIKE_OUT: '삼진',
        FOUL: '파울',
    };

    const els = {
        title: document.getElementById('spectateTitle'),
        sub: document.getElementById('spectateSub'),
        pitcher: document.getElementById('spectatePitcher'),
        batter: document.getElementById('spectateBatter'),
        phase: document.getElementById('spectatePhase'),
        startCoord: document.getElementById('spectateStartCoord'),
        coordValue: document.getElementById('spectateCoordValue'),
        coordGrid: document.getElementById('spectateCoordGrid'),
        lastResult: document.getElementById('spectateLastResult'),
        lastResultText: document.getElementById('spectateLastResultText'),
        lastResultDetail: document.getElementById('spectateLastResultDetail'),
        log: document.getElementById('spectateLog'),
        status: document.getElementById('spectateStatus'),
    };

    let pollTimer = null;
    let lastPhaseKey = '';
    let navigating = false;
    let startCoord = 0;
    let finalCoord = 0;
    const connectionTracker = createConnectionTracker
        ? createConnectionTracker(2500)
        : {
            get: () => (BluffBallGameWs.isConnected() ? true : null),
            noteConnected: () => {},
            noteDisconnected: () => {},
        };

    function setStatus(text) {
        if (els.status) {
            els.status.textContent = text || '';
        }
    }

    function pushLog(message) {
        if (!els.log || !message) {
            return;
        }
        const li = document.createElement('li');
        li.textContent = `[${new Date().toLocaleTimeString()}] ${message}`;
        els.log.prepend(li);
        while (els.log.children.length > 12) {
            els.log.removeChild(els.log.lastChild);
        }
    }

    function stopPoll() {
        if (pollTimer) {
            clearInterval(pollTimer);
            pollTimer = null;
        }
    }

    function renderCoordGrid() {
        if (!els.coordGrid) {
            return;
        }
        els.coordGrid.innerHTML = '';
        for (let n = 1; n <= 25; n += 1) {
            const cell = document.createElement('div');
            cell.className = 'coord-btn spectate-coord-cell';
            if (STRIKE_ZONE.has(n)) {
                cell.classList.add('strike-zone');
            }
            if (startCoord === n) {
                cell.classList.add('start-selected');
            }
            if (finalCoord === n) {
                cell.classList.add('final-selected');
            }
            cell.textContent = String(n);
            els.coordGrid.appendChild(cell);
        }
    }

    let lastLoggedResultKey = '';

    function showLastResult(result) {
        if (!els.lastResult || !result?.turnResult) {
            if (els.lastResult) {
                els.lastResult.hidden = true;
            }
            return;
        }
        els.lastResult.hidden = false;
        const label = RESULT_LABEL[result.turnResult] || result.turnResult;
        els.lastResultText.textContent = label;
        const parts = [];
        if (result.pitchCardName) {
            parts.push(`구종 ${result.pitchCardName}`);
        }
        if (result.finalCoordinateNumber > 0) {
            parts.push(`최종 좌표 ${result.finalCoordinateNumber}`);
            finalCoord = Number(result.finalCoordinateNumber);
        }
        if (result.pitchTiming) {
            parts.push(`타이밍 ${result.pitchTiming}`);
        }
        els.lastResultDetail.textContent = parts.join(' · ');
        renderCoordGrid();

        const key = `${result.turnNumber || ''}|${result.turnResult}|${result.finalCoordinateNumber || 0}`;
        if (key !== lastLoggedResultKey) {
            pushLog(`턴 결과 · ${label}` + (result.finalCoordinateNumber > 0 ? ` · 최종 ${result.finalCoordinateNumber}` : ''));
            lastLoggedResultKey = key;
        }
    }

    function refreshHudFromBoard(board) {
        const hud = document.getElementById('broadcastHud');
        if (!hud || !board || !renderBroadcastHud) {
            return;
        }
        renderBroadcastHud(hud, {
            inning: board.inning,
            isTop: board.isTop,
            homeScore: board.homeScore,
            awayScore: board.awayScore,
            balls: board.balls,
            strikes: board.strikes,
            outs: board.outs,
            firstBase: board.firstBase,
            secondBase: board.secondBase,
            thirdBase: board.thirdBase,
        });
    }

    function renderSession(session) {
        if (!session) {
            return;
        }
        const turn = session.turn || {};
        const pitcherWho = describeActor(session.pitcherUserId);
        const batterWho = describeActor(session.batterUserId);

        els.pitcher.textContent = `${pitcherWho.label} (#${session.pitcherUserId ?? '-'})`;
        els.batter.textContent = `${batterWho.label} (#${session.batterUserId ?? '-'})`;

        let phaseText = '준비';
        let title = '봇 플레이를 보는 중';
        let sub = '우리 팀 봇 행동을 이 화면에서 확인합니다.';
        let tone = 'spectate';

        startCoord = Number(turn.startCoordinateNumber) || 0;

        if (!turn.pitcherSelectionComplete) {
            phaseText = '투수 구종·좌표 선택 중';
            startCoord = 0;
            els.coordValue.textContent = '투수 선택 대기';
            if (pitcherWho.kind === 'teammate') {
                title = '관전 — 우리 팀 투수 봇';
                sub = '아군 투수 봇이 구종과 시작 좌표를 고르는 중입니다.';
            } else if (pitcherWho.kind === 'opponent') {
                title = '관전 — 상대 투수 봇';
                sub = '상대 투수가 투구하는 동안 좌표판을 지켜보세요.';
                tone = 'wait';
            } else if (pitcherWho.kind === 'me') {
                title = '내 차례 — 투수로 이동합니다';
                tone = 'mine';
            }
        } else if (!turn.batterSelectionComplete) {
            phaseText = '타자 타격 선택 중';
            els.coordValue.textContent = startCoord > 0
                ? String(startCoord)
                : '좌표 확인 중';
            if (batterWho.kind === 'teammate') {
                title = '관전 — 우리 팀 타자 봇';
                sub = `시작 좌표 ${startCoord || '-'} · 아군 타자 봇이 스윙/타이밍을 고르는 중`;
            } else if (batterWho.kind === 'opponent') {
                title = '관전 — 상대 타자 봇';
                sub = `시작 좌표 ${startCoord || '-'} · 상대 타자가 타격하는 중`;
                tone = 'wait';
            } else if (batterWho.kind === 'me') {
                title = '내 차례 — 타격 화면으로 이동합니다';
                tone = 'mine';
            }
        } else {
            phaseText = '턴 결과 처리 중';
            title = '턴 결과 확인';
            tone = 'info';
            els.coordValue.textContent = startCoord > 0 ? String(startCoord) : '-';
        }

        renderCoordGrid();
        showLastResult(session.lastTurnResult);

        els.phase.textContent = phaseText;
        els.title.textContent = title;
        els.sub.textContent = sub;

        const phaseKey = `${phaseText}|${session.pitcherUserId}|${session.batterUserId}|${turn.pitcherSelectionComplete}|${turn.batterSelectionComplete}|${startCoord}|${session.lastTurnResult?.turnResult || ''}`;
        if (phaseKey !== lastPhaseKey) {
            pushLog(title + (startCoord > 0 ? ` · 시작 ${startCoord}` : ''));
            lastPhaseKey = phaseKey;
        }

        setTurnBanner({
            tone,
            title,
            sub,
            connected: connectionTracker.get(),
        });
    }

    async function tick(matchSessionId) {
        if (navigating) {
            return;
        }
        const session = await fetchSessionState(matchSessionId);
        if (!session) {
            setStatus('세션 조회 실패 — 재시도 중');
            return;
        }
        applySessionState(session);
        renderSession(session);
        refreshHudFromBoard(session.board);

        const decision = routeBySession(session);
        if (decision?.page && decision.page !== '/game-test/BotSpectate.html') {
            navigating = true;
            stopPoll();
            setStatus(`이동: ${decision.page}`);
            goWithMatch(decision.page);
        } else {
            setStatus('관전 중 — 좌표판·직전 판정을 이 화면에서 확인');
        }
    }

    function handlePitcherReady({ event }) {
        if (event?.startCoordinateNumber == null) {
            return;
        }
        startCoord = Number(event.startCoordinateNumber);
        els.coordValue.textContent = String(startCoord);
        renderCoordGrid();
        pushLog(`PitcherReady · 시작 좌표 ${startCoord}`);
    }

    function handleTurnResult({ event }) {
        // 관전 중에는 페이지 이동하지 않는다 (WS 끊김 루프 방지).
        if (event?.finalCoordinateNumber > 0) {
            finalCoord = Number(event.finalCoordinateNumber);
        }
        showLastResult({
            turnNumber: event?.turnNumber,
            turnResult: event?.turnResult,
            finalCoordinateNumber: event?.finalCoordinateNumber,
            pitchCardName: event?.pitchCardName,
            pitchTiming: event?.pitchTiming,
        });
        const hud = document.getElementById('broadcastHud');
        if (hud && renderBroadcastHud && event?.inning != null) {
            renderBroadcastHud(hud, {
                inning: event.inning,
                isTop: event.isTop,
                homeScore: event.homeScore,
                awayScore: event.awayScore,
                balls: event.balls,
                strikes: event.strikes,
                outs: event.outs,
                firstBase: event.firstBase,
                secondBase: event.secondBase,
                thirdBase: event.thirdBase,
            });
        }

        const matchId = getMatchSessionId();
        if (matchId) {
            tick(matchId).catch(() => {});
        }
    }

    function handleGameEnd() {
        navigating = true;
        stopPoll();
        goWithMatch('/game-test/GameEnd.html');
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

        renderCoordGrid();

        BluffBallGameWs.attachInGamePhaseGuard();
        BluffBallGameWs.on(BluffBallGameWs.EVENT.PITCHER_READY, handlePitcherReady);
        BluffBallGameWs.on(BluffBallGameWs.EVENT.TURN_RESULT, handleTurnResult);
        BluffBallGameWs.on(BluffBallGameWs.EVENT.GAME_END, handleGameEnd);

        BluffBallGameWs.connect({
            matchSessionId,
            reconnectDelay: 1500,
            onConnect: () => {
                connectionTracker.noteConnected();
                setStatus('연결됨 — 관전 중');
            },
            onDisconnect: () => {
                connectionTracker.noteDisconnected();
                setStatus('연결 재시도 중…');
            },
        });

        mountInGameHud();
        await tick(matchSessionId);
        pollTimer = setInterval(() => {
            tick(matchSessionId).catch(() => {});
            pollTurnBanner(matchSessionId, () => connectionTracker.get()).catch(() => {});
        }, 1000);
    }

    window.addEventListener('beforeunload', stopPoll);
    init().catch((e) => setStatus(e.message || String(e)));
})();
