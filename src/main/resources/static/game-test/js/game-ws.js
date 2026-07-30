(() => {
    const STORAGE_PITCH_HAND = 'bluffball.pitchHand';
    const STORAGE_TURN_RESULT = 'bluffball.batterResult';
    const STORAGE_GAME_END = 'bluffball.gameEnd';
    const STORAGE_MULLIGAN_DONE = 'bluffball.mulliganDone';
    const STORAGE_MY_MULLIGAN_DONE = 'bluffball.myMulliganDone';
    const STORAGE_ALL_MULLIGAN_READY = 'bluffball.allMulliganReady';

    /** 서버 → 클라이언트 이벤트 타입 (토픽·페이로드 기준) */
    const EVENT = {
        CARD_HAND: 'cardHand',
        ROLE_CHANGED: 'roleChanged',
        PITCHER_READY: 'pitcherReady',
        PITCHER_SUBSTITUTED: 'pitcherSubstituted',
        TURN_RESULT: 'turnResult',
        GAME_END: 'gameEnd',
        UNKNOWN: 'unknown',
    };

    let stompClient = null;
    let matchSessionId = null;
    let connected = false;
    let subscriptions = [];
    const listeners = new Map();

    function topicGame(id) {
        return `/topic/game/${id}`;
    }

    function topicResult(id) {
        return `/topic/game/${id}/result`;
    }

    function topicEnd(id) {
        return `/topic/game/${id}/end`;
    }

    function appDestination(action) {
        return `/app/game/${matchSessionId}/${action}`;
    }

    /** 토픽·JSON 페이로드로 이벤트 타입 판별 */
    function parseIncomingEvent(rawBody, channel) {
        let event;
        try {
            event = JSON.parse(rawBody);
        } catch (_) {
            return { type: EVENT.UNKNOWN, event: null, rawBody };
        }

        if (channel === 'result') {
            return { type: EVENT.TURN_RESULT, event, rawBody };
        }
        if (channel === 'end') {
            return { type: EVENT.GAME_END, event, rawBody };
        }

        if (Array.isArray(event.cards)) {
            return { type: EVENT.CARD_HAND, event, rawBody };
        }
        if (event.newPitcherUserId != null && event.demotedPitcherUserId != null) {
            return { type: EVENT.PITCHER_SUBSTITUTED, event, rawBody };
        }
        if (typeof event.pitcherUserId === 'number' && event.turnResult == null) {
            return { type: EVENT.ROLE_CHANGED, event, rawBody };
        }
        if (typeof event.startCoordinateNumber === 'number') {
            return { type: EVENT.PITCHER_READY, event, rawBody };
        }

        return { type: EVENT.UNKNOWN, event, rawBody };
    }

    function emit(type, parsed, stompMessage) {
        const payload = { ...parsed, stompMessage };
        (listeners.get(type) || []).forEach((fn) => fn(payload));
        (listeners.get('*') || []).forEach((fn) => fn(type, payload));
    }

    function clearSubscriptions() {
        subscriptions.forEach((sub) => {
            try {
                sub.unsubscribe();
            } catch (_) {
                /* ignore */
            }
        });
        subscriptions = [];
    }

    function handleRawMessage(channel) {
        return (message) => {
            const parsed = parseIncomingEvent(message.body, channel);
            if (parsed.type === EVENT.CARD_HAND) {
                persistCardHand(parsed.event);
            }
            if (parsed.type === EVENT.ROLE_CHANGED) {
                persistRoleChanged(parsed.event);
            }
            if (parsed.type === EVENT.PITCHER_SUBSTITUTED) {
                persistRoleChanged(parsed.event);
            }
            if (parsed.type === EVENT.TURN_RESULT) {
                persistTurnResult(parsed.event);
            }
            if (parsed.type === EVENT.GAME_END) {
                persistGameEnd(parsed.event);
            }
            emit(parsed.type, parsed, message);
        };
    }

    function subscribeAll(client, id) {
        subscriptions = [
            client.subscribe(topicGame(id), handleRawMessage('game')),
            client.subscribe(topicResult(id), handleRawMessage('result')),
            client.subscribe(topicEnd(id), handleRawMessage('end')),
        ];
    }

    function isCardHandForMe(event) {
        const myId = window.BluffBallRole?.getMyUserId?.();
        if (myId == null) {
            return false;
        }
        if (event?.targetUserId == null) {
            return true;
        }
        return Number(event.targetUserId) === myId;
    }

    function resetMulliganPhase() {
        sessionStorage.setItem(STORAGE_MY_MULLIGAN_DONE, '0');
        sessionStorage.setItem(STORAGE_ALL_MULLIGAN_READY, '0');
        setMulliganDone(false);
    }

    function setMyMulliganDone(done) {
        sessionStorage.setItem(STORAGE_MY_MULLIGAN_DONE, done ? '1' : '0');
    }

    function isMyMulliganDone() {
        return sessionStorage.getItem(STORAGE_MY_MULLIGAN_DONE) === '1';
    }

    function setAllMulliganReady(ready) {
        sessionStorage.setItem(STORAGE_ALL_MULLIGAN_READY, ready ? '1' : '0');
        if (ready) {
            setMulliganDone(true);
        }
    }

    function isAllMulliganReady() {
        if (sessionStorage.getItem(STORAGE_ALL_MULLIGAN_READY) === '1') {
            return true;
        }
        // 리그는 인게임 멀리건 없음 — 플래그가 없어도 play-ready
        const mode = sessionStorage.getItem('bluffball.gameMode');
        return mode === 'COMPACT_LEAGUE' || mode === 'FULL_LEAGUE';
    }

    /** CardHandEvent — 게임 시작 드로우·멀리건 확정 시에만 sessionStorage 갱신 */
    function persistCardHand(event) {
        if (!event?.cards || !isCardHandForMe(event)) {
            return;
        }

        sessionStorage.setItem(STORAGE_PITCH_HAND, JSON.stringify(event.cards));

        if (event.pitcherUserId != null) {
            sessionStorage.setItem('bluffball.pitcherUserId', String(event.pitcherUserId));
            window.BluffBallRole?.syncRoleFromPitcherUserId?.(event.pitcherUserId);
        }

        if (event.fromMulligan === true) {
            setMyMulliganDone(true);
            if (event.allMulliganReady === true) {
                setAllMulliganReady(true);
            }
        } else if (!isAllMulliganReady()) {
            // 리그 사전선택 핸드는 멀리건 플래그를 리셋하지 않는다
            const mode = sessionStorage.getItem('bluffball.gameMode');
            if (mode !== 'COMPACT_LEAGUE' && mode !== 'FULL_LEAGUE') {
                resetMulliganPhase();
            }
        }
    }

    function persistRoleChanged(event) {
        if (event?.pitcherUserId == null) {
            return;
        }
        sessionStorage.setItem('bluffball.pitcherUserId', String(event.pitcherUserId));
        window.BluffBallRole?.syncRoleFromPitcherUserId?.(event.pitcherUserId);
    }

    /** CardHandEvent 수신 후 역할·멀리건 상태에 맞는 화면으로 이동 */
    function routeAfterCardHand(event) {
        if (!isCardHandForMe(event)) {
            return;
        }
        if (event?.pitcherUserId != null) {
            BluffBallRole.syncRoleFromPitcherUserId(event.pitcherUserId);
        }

        const id = getMatchSessionId() || sessionStorage.getItem('bluffball.matchSessionId');
        if (!id) {
            return;
        }

        const qs = `?matchSessionId=${encodeURIComponent(id)}`;

        // 리그 또는 이미 멀리건 완료면 인게임으로
        if (!event.allMulliganReady && !isAllMulliganReady()) {
            window.location.href = `/game-test/Mulligan.html${qs}`;
            return;
        }

        if (BluffBallRole.isPitcher()) {
            window.location.href = `/game-test/PitcherSelect.html${qs}`;
        } else {
            window.location.href = `/game-test/BatterWait.html${qs}`;
        }
    }

    function getStoredPitchHand() {
        try {
            const raw = sessionStorage.getItem(STORAGE_PITCH_HAND);
            return raw ? JSON.parse(raw) : null;
        } catch (_) {
            return null;
        }
    }

    function turnResultToDisplay(event) {
        const stored = getStoredTurnResult() || {};
        return {
            ...stored,
            matchSessionId: matchSessionId || stored.matchSessionId,
            turnResult: event.turnResult,
            finalCoordinateNumber: event.finalCoordinateNumber,
            pitchTiming: event.pitchTiming,
            pitchCardName: event.pitchCardName,
            diceResults: event.diceResults,
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
            pitcherUserId: event.pitcherUserId,
            halfInningChanged: event.halfInningChanged === true,
            gameOver: event.gameOver === true || stored.gameOver === true,
        };
    }

    function persistTurnResult(event) {
        if (event.pitcherUserId != null) {
            sessionStorage.setItem('bluffball.pitcherUserId', String(event.pitcherUserId));
            window.BluffBallRole?.syncRoleFromPitcherUserId?.(event.pitcherUserId);
        }
        sessionStorage.setItem(STORAGE_TURN_RESULT, JSON.stringify(turnResultToDisplay(event)));
    }

    /** 턴 결과 이후 다음 화면으로 이동 (사람 조작 / 아군·상대 봇 관전) */
    function navigateAfterTurnResult(data) {
        const go = window.BluffBallNav?.goWithMatch
            || ((path) => {
                const id = sessionStorage.getItem('bluffball.matchSessionId');
                window.location.href = id
                    ? `${path}?matchSessionId=${encodeURIComponent(id)}`
                    : path;
            });

        if (data?.pitcherUserId != null) {
            window.BluffBallRole?.syncRoleFromPitcherUserId?.(data.pitcherUserId);
        } else {
            window.BluffBallRole?.ensureRoleSyncedFromStorage?.();
        }

        const endData = getStoredGameEnd();
        if (data?.gameOver || endData) {
            go('/game-test/GameEnd.html');
            return;
        }

        const matchId = getMatchSessionId() || sessionStorage.getItem('bluffball.matchSessionId');
        if (window.BluffBallNav?.fetchSessionState && matchId) {
            window.BluffBallNav.fetchSessionState(matchId).then((session) => {
                if (session) {
                    window.BluffBallNav.applySessionState?.(session);
                    const decision = window.BluffBallNav.routeBySession?.(session);
                    if (decision?.page) {
                        go(decision.page);
                        return;
                    }
                }
                go('/game-test/BotSpectate.html');
            }).catch(() => go('/game-test/BotSpectate.html'));
            return;
        }

        go('/game-test/BotSpectate.html');
    }

    /** 공수 교대 — 세션 기준 라우팅 */
    function routeAfterRoleChange(event) {
        persistRoleChanged(event);

        const id = getMatchSessionId() || sessionStorage.getItem('bluffball.matchSessionId');
        if (!id) {
            return;
        }

        if (window.BluffBallNav?.fetchSessionState) {
            window.BluffBallNav.fetchSessionState(id).then((session) => {
                if (session) {
                    window.BluffBallNav.applySessionState?.(session);
                    const decision = window.BluffBallNav.routeBySession?.(session);
                    if (decision?.page) {
                        window.BluffBallNav.goWithMatch(decision.page, id);
                        return;
                    }
                }
                window.BluffBallNav.goWithMatch('/game-test/BotSpectate.html', id);
            }).catch(() => {
                window.BluffBallNav.goWithMatch('/game-test/BotSpectate.html', id);
            });
            return;
        }

        const qs = `?matchSessionId=${encodeURIComponent(id)}`;
        if (BluffBallRole.isPitcher()) {
            window.location.href = `/game-test/PitcherSelect.html${qs}`;
        } else {
            window.location.href = `/game-test/BatterWait.html${qs}`;
        }
    }

    /** 게임 중 공수 교대·초기 드로우 이벤트 처리 */
    function attachInGamePhaseGuard() {
        on(EVENT.ROLE_CHANGED, ({ event }) => {
            routeAfterRoleChange(event);
        });
        on(EVENT.CARD_HAND, ({ event }) => {
            if (!isCardHandForMe(event)) {
                return;
            }
            if (event.fromMulligan === true) {
                return;
            }
            if (!isAllMulliganReady()) {
                routeAfterCardHand(event);
            }
        });
    }

    function getStoredTurnResult() {
        try {
            const raw = sessionStorage.getItem(STORAGE_TURN_RESULT);
            return raw ? JSON.parse(raw) : null;
        } catch (_) {
            return null;
        }
    }

    function persistGameEnd(event) {
        const base = getStoredTurnResult() || {};
        const merged = {
            ...base,
            matchSessionId: matchSessionId || base.matchSessionId,
            homeScore: event.homeScore,
            awayScore: event.awayScore,
            winnerUserId: event.winnerUserId,
            gameOver: true,
        };
        sessionStorage.setItem(STORAGE_TURN_RESULT, JSON.stringify(merged));
        sessionStorage.setItem(STORAGE_GAME_END, JSON.stringify(merged));
    }

    function getStoredGameEnd() {
        try {
            const raw = sessionStorage.getItem(STORAGE_GAME_END);
            return raw ? JSON.parse(raw) : null;
        } catch (_) {
            return null;
        }
    }

    function setMulliganDone(done) {
        sessionStorage.setItem(STORAGE_MULLIGAN_DONE, done ? '1' : '0');
    }

    function isMulliganDone() {
        return sessionStorage.getItem(STORAGE_MULLIGAN_DONE) === '1';
    }

    function connectGame(options = {}) {
        return connect(options);
    }

    /**
     * 인게임 WebSocket 연결 + 구독.
     *
     * @param {object} options
     * @param {string} options.matchSessionId
     * @param {number} [options.reconnectDelay=5000]
     * @param {function} [options.onConnect]
     * @param {function} [options.onDisconnect]
     * @param {function} [options.onError] (frame) => void
     */
    function connect(options = {}) {
        if (typeof StompJs === 'undefined' || typeof SockJS === 'undefined') {
            throw new Error('STOMP/SockJS 라이브러리가 로드되지 않았습니다.');
        }

        const id = options.matchSessionId;
        if (!id) {
            throw new Error('matchSessionId가 필요합니다.');
        }

        if (stompClient?.active && matchSessionId === id && connected) {
            options.onConnect?.();
            return;
        }

        disconnect();

        matchSessionId = id;
        const accessToken = BluffBallWs.requireLoginToken();
        const reconnectDelay = options.reconnectDelay ?? 5000;

        stompClient = BluffBallWs.createStompClient(accessToken, {
            reconnectDelay,
            onConnect: () => {
                connected = true;
                subscribeAll(stompClient, id);
                options.onConnect?.();
            },
            onStompError: (frame) => {
                connected = false;
                options.onError?.(frame);
                stompClient?.deactivate();
            },
            onWebSocketClose: () => {
                connected = false;
                clearSubscriptions();
                options.onDisconnect?.();
            },
        });

        stompClient.activate();
    }

    function disconnect() {
        clearSubscriptions();
        if (stompClient?.active) {
            stompClient.deactivate();
        }
        stompClient = null;
        connected = false;
    }

    function isConnected() {
        return connected && stompClient?.connected === true;
    }

    function getMatchSessionId() {
        return matchSessionId;
    }

    /**
     * 본番 인게임 액션 전송.
     *
     * @param {string} action setup-numbers | cards/mulligan | pitcher/select-card | batter/select-card
     * @param {object} body JSON 직렬화 대상
     */
    function publish(action, body) {
        if (!isConnected()) {
            throw new Error('WebSocket이 연결되지 않았습니다.');
        }
        stompClient.publish({
            destination: appDestination(action),
            body: JSON.stringify(body),
        });
    }

    /** 이벤트 리스너 등록. type에 '*' 를 주면 모든 이벤트 수신 */
    function on(eventType, handler) {
        if (!listeners.has(eventType)) {
            listeners.set(eventType, []);
        }
        listeners.get(eventType).push(handler);
    }

    function off(eventType, handler) {
        const list = listeners.get(eventType);
        if (!list) {
            return;
        }
        const idx = list.indexOf(handler);
        if (idx >= 0) {
            list.splice(idx, 1);
        }
    }

    window.addEventListener('beforeunload', () => {
        // 페이지 이동 시 즉시 deactivate하지 않는다.
        // (짧은 전환에서 presence grace로 버티고, 다음 페이지가 재연결)
    });

    window.BluffBallGameWs = {
        EVENT,
        connect,
        connectGame,
        disconnect,
        publish,
        isConnected,
        getMatchSessionId,
        on,
        off,
        persistCardHand,
        getStoredPitchHand,
        isCardHandForMe,
        routeAfterCardHand,
        navigateAfterTurnResult,
        attachInGamePhaseGuard,
        routeAfterRoleChange,
        persistRoleChanged,
        resetMulliganPhase,
        setMyMulliganDone,
        isMyMulliganDone,
        setAllMulliganReady,
        isAllMulliganReady,
        persistTurnResult,
        getStoredTurnResult,
        persistGameEnd,
        getStoredGameEnd,
        setMulliganDone,
        isMulliganDone,
        turnResultToDisplay,
        topicGame,
        topicResult,
        topicEnd,
    };
})();
