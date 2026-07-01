(() => {
    const STORAGE_PITCH_HAND = 'bluffball.pitchHand';

    /** 서버 → 클라이언트 이벤트 타입 (토픽·페이로드 기준) */
    const EVENT = {
        CARD_HAND: 'cardHand',
        PITCHER_READY: 'pitcherReady',
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

    /**
     * CardHandEvent — 멀리건·투수 선택 화면에서 sessionStorage로 재사용.
     */
    function persistCardHand(event) {
        if (!event?.cards) {
            return;
        }
        sessionStorage.setItem(STORAGE_PITCH_HAND, JSON.stringify(event.cards));
    }

    function getStoredPitchHand() {
        try {
            const raw = sessionStorage.getItem(STORAGE_PITCH_HAND);
            return raw ? JSON.parse(raw) : null;
        } catch (_) {
            return null;
        }
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
        disconnect();
    });

    window.BluffBallGameWs = {
        EVENT,
        connect,
        disconnect,
        publish,
        isConnected,
        getMatchSessionId,
        on,
        off,
        persistCardHand,
        getStoredPitchHand,
        topicGame,
        topicResult,
        topicEnd,
    };
})();
