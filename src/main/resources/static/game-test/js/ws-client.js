(() => {
    /**
     * OAuth 로그인 Access Token — 본番 매칭·인게임 WebSocket용.
     * @throws {Error} 토큰 없을 때
     */
    function requireLoginToken() {
        const token = window.BluffBallAuth?.getAccessToken?.();
        if (!token) {
            throw new Error('로그인이 필요합니다.');
        }
        return token;
    }

    /**
     * STOMP 클라이언트 생성 (SockJS + JWT CONNECT).
     *
     * @param {string} accessToken JWT Access Token
     * @param {object} handlers onConnect, onStompError, onWebSocketClose, reconnectDelay
     */
    function createStompClient(accessToken, handlers = {}) {
        return new StompJs.Client({
            webSocketFactory: () => new SockJS('/ws'),
            connectHeaders: {
                Authorization: `Bearer ${accessToken}`,
            },
            reconnectDelay: handlers.reconnectDelay ?? 0,
            onConnect: handlers.onConnect,
            onStompError: handlers.onStompError,
            onWebSocketClose: handlers.onWebSocketClose,
        });
    }

    window.BluffBallWs = {
        requireLoginToken,
        createStompClient,
    };
})();
