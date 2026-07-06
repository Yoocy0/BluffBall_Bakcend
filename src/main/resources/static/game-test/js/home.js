(() => {
    const MATCH_COMPLETE_DELAY_SEC = 3;

    const loginScreen = document.getElementById('loginScreen');
    const homeScreen = document.getElementById('homeScreen');
    const matchingScreen = document.getElementById('matchingScreen');
    const matchCompleteModal = document.getElementById('matchCompleteModal');
    const matchCompleteCountdown = document.getElementById('matchCompleteCountdown');

    const btnKakaoLogin = document.getElementById('btnKakaoLogin');
    const btnGoogleLogin = document.getElementById('btnGoogleLogin');
    const loginError = document.getElementById('loginError');
    const userInfo = document.getElementById('userInfo');
    const homeError = document.getElementById('homeError');
    const btnLogout = document.getElementById('btnLogout');

    const btnModeSingle = document.getElementById('btnModeSingle');
    const matchingTimer = document.getElementById('matchingTimer');
    const matchingError = document.getElementById('matchingError');
    const btnCancelMatch = document.getElementById('btnCancelMatch');

    const state = {
        stompClient: null,
        timerInterval: null,
        matchingStartedAt: null,
        matchHandled: false,
        /** 큐 WAITING 상태 — 매칭 성사 전에만 true */
        waitingInQueue: false,
    };

    function showScreen(screen) {
        loginScreen.hidden = screen !== 'login';
        homeScreen.hidden = screen !== 'home';
        matchingScreen.hidden = screen !== 'matching';
    }

    function renderLoginState() {
        if (BluffBallAuth.isLoggedIn()) {
            showScreen('home');
            const userId = BluffBallAuth.getUserIdFromToken();
            userInfo.textContent = userId ? `로그인됨 · userId ${userId}` : '로그인됨';
        } else {
            showScreen('login');
        }
    }

    function showError(el, message) {
        if (!el) {
            return;
        }
        el.textContent = message;
        el.hidden = !message;
    }

    function formatElapsed(ms) {
        const totalSec = Math.floor(ms / 1000);
        const min = String(Math.floor(totalSec / 60)).padStart(2, '0');
        const sec = String(totalSec % 60).padStart(2, '0');
        return `${min}:${sec}`;
    }

    function startMatchingTimer() {
        stopMatchingTimer();
        state.matchingStartedAt = Date.now();
        matchingTimer.textContent = '00:00';
        state.timerInterval = setInterval(() => {
            matchingTimer.textContent = formatElapsed(Date.now() - state.matchingStartedAt);
        }, 1000);
    }

    function stopMatchingTimer() {
        if (state.timerInterval) {
            clearInterval(state.timerInterval);
            state.timerInterval = null;
        }
    }

    function disconnectMatchWs() {
        if (state.stompClient?.active) {
            state.stompClient.deactivate();
        }
        state.stompClient = null;
    }

    /** REST — 매칭 큐 취소 요청 */
    async function cancelMatchQueueRequest(keepalive = false) {
        const token = BluffBallWs.requireLoginToken();
        const res = await fetch('/api/v1/match/queue/cancel', {
            method: 'DELETE',
            headers: {
                Authorization: `Bearer ${token}`,
            },
            keepalive,
        });
        if (!res.ok && res.status !== 204 && res.status !== 404) {
            const body = await res.json().catch(() => ({}));
            throw new Error(body.message || `취소 실패 (HTTP ${res.status})`);
        }
    }

    /**
     * WAITING 큐 등록 해제.
     * @param {{ silent?: boolean, keepalive?: boolean }} options
     */
    async function releaseQueueIfWaiting(options = {}) {
        if (!state.waitingInQueue) {
            return;
        }
        state.waitingInQueue = false;
        try {
            await cancelMatchQueueRequest(options.keepalive === true);
        } catch (e) {
            if (!options.silent) {
                throw e;
            }
        }
    }

    /** 페이지 이탈 시 keepalive로 큐 취소 (비동기 완료 대기 불가) */
    function releaseQueueOnPageHide() {
        if (!state.waitingInQueue) {
            return;
        }
        const token = window.BluffBallAuth?.getAccessToken?.();
        if (!token) {
            return;
        }
        state.waitingInQueue = false;
        fetch('/api/v1/match/queue/cancel', {
            method: 'DELETE',
            headers: {
                Authorization: `Bearer ${token}`,
            },
            keepalive: true,
        }).catch(() => {
            /* ignore */
        });
    }

    function navigateToSetup(matchSessionId) {
        state.waitingInQueue = false;
        sessionStorage.setItem('bluffball.matchSessionId', matchSessionId);
        sessionStorage.removeItem('bluffball.mulliganDone');
        sessionStorage.removeItem('bluffball.myMulliganDone');
        sessionStorage.removeItem('bluffball.allMulliganReady');
        sessionStorage.removeItem('bluffball.pitcherUserId');
        sessionStorage.removeItem('bluffball.pitchHand');
        sessionStorage.removeItem('bluffball.batterResult');
        sessionStorage.removeItem('bluffball.gameEnd');
        sessionStorage.removeItem('bluffball.mySetupNumbers');
        sessionStorage.removeItem('bluffball.doubleJudgment');
        window.location.href =
            `/game-test/SetupNumber.html?matchSessionId=${encodeURIComponent(matchSessionId)}`;
    }

    function handleMatchComplete(matchSessionId, role) {
        if (state.matchHandled) {
            return;
        }
        state.matchHandled = true;
        state.waitingInQueue = false;

        if (role) {
            BluffBallRole.setRole(role);
        }

        stopMatchingTimer();
        disconnectMatchWs();

        matchCompleteModal.hidden = false;
        let remaining = MATCH_COMPLETE_DELAY_SEC;
        matchCompleteCountdown.textContent = String(remaining);

        const countdownInterval = setInterval(() => {
            remaining -= 1;
            if (remaining <= 0) {
                clearInterval(countdownInterval);
                navigateToSetup(matchSessionId);
                return;
            }
            matchCompleteCountdown.textContent = String(remaining);
        }, 1000);
    }

    async function requestJoinMatchQueue() {
        const token = BluffBallWs.requireLoginToken();
        const res = await fetch('/api/v1/match/queue/join', {
            method: 'POST',
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        const body = await res.json().catch(() => ({}));
        return { res, body };
    }

    async function joinMatchQueue() {
        let { res, body } = await requestJoinMatchQueue();

        if (res.status === 409) {
            await cancelMatchQueueRequest();
            ({ res, body } = await requestJoinMatchQueue());
        }

        if (res.status === 409) {
            throw new Error(body.message || '이미 매칭 큐에 등록되어 있습니다.');
        }
        if (!res.ok && res.status !== 202) {
            throw new Error(body.message || `매칭 큐 진입 실패 (HTTP ${res.status})`);
        }

        if (body.status === 'MATCHED' && body.matchSessionId) {
            handleMatchComplete(body.matchSessionId, 'batter');
            return 'matched';
        }

        return 'waiting';
    }

    function abortMatchingDueToDisconnect(message) {
        if (state.matchHandled || !state.waitingInQueue) {
            disconnectMatchWs();
            return;
        }

        stopMatchingTimer();
        disconnectMatchWs();

        releaseQueueIfWaiting({ silent: true }).finally(() => {
            state.matchHandled = false;
            showScreen('home');
            showError(matchingError, '');
            showError(homeError, message);
        });
    }

    function connectMatchNotificationWs() {
        const userId = BluffBallAuth.getUserIdFromToken();
        if (!userId) {
            throw new Error('userId를 확인할 수 없습니다.');
        }

        const token = BluffBallWs.requireLoginToken();
        disconnectMatchWs();

        const client = BluffBallWs.createStompClient(token, {
            onConnect: () => {
                client.subscribe(`/topic/user/${userId}/match`, (message) => {
                    try {
                        const event = JSON.parse(message.body);
                        if (event.matchSessionId) {
                            handleMatchComplete(event.matchSessionId, 'pitcher');
                        }
                    } catch (_) {
                        showError(matchingError, '매칭 알림 파싱 실패');
                    }
                });
            },
            onStompError: (frame) => {
                abortMatchingDueToDisconnect(
                    frame.headers['message'] || 'WebSocket 오류로 매칭 대기를 취소했습니다.',
                );
            },
            onWebSocketClose: () => {
                abortMatchingDueToDisconnect('매칭 연결이 끊어져 대기를 취소했습니다.');
            },
        });

        client.activate();
        state.stompClient = client;
    }

    async function startSingleModeMatching() {
        showError(homeError, '');
        showError(matchingError, '');
        state.matchHandled = false;
        state.waitingInQueue = false;

        showScreen('matching');
        startMatchingTimer();

        try {
            const result = await joinMatchQueue();
            if (result === 'waiting') {
                state.waitingInQueue = true;
                connectMatchNotificationWs();
            }
        } catch (e) {
            stopMatchingTimer();
            disconnectMatchWs();
            state.waitingInQueue = false;
            showScreen('home');
            showError(homeError, e.message || String(e));
        }
    }

    async function cancelMatching() {
        showError(matchingError, '');
        try {
            await releaseQueueIfWaiting();
        } catch (e) {
            showError(matchingError, e.message || String(e));
            return;
        }

        stopMatchingTimer();
        disconnectMatchWs();
        state.matchHandled = false;
        showScreen('home');
    }

    async function handleOAuthLogin(provider) {
        showError(loginError, '');
        btnKakaoLogin.disabled = true;
        btnGoogleLogin.disabled = true;

        try {
            await BluffBallAuth.startOAuthLogin(provider);
        } catch (e) {
            showError(loginError, e.message || String(e));
            btnKakaoLogin.disabled = false;
            btnGoogleLogin.disabled = false;
        }
    }

    async function showOAuthSetupHint() {
        try {
            const config = await BluffBallAuth.fetchOAuthConfig();
            btnKakaoLogin.disabled = !config.kakaoConfigured;
            btnGoogleLogin.disabled = !config.googleConfigured;
            if (!config.kakaoConfigured && !config.googleConfigured) {
                showError(loginError,
                    'OAuth 키가 서버에 설정되지 않았습니다. oauth-local.yml 설정 후 서버를 재시작하세요.');
            }
        } catch (e) {
            showError(loginError, `OAuth 설정 확인 실패: ${e.message}`);
        }
    }

    btnKakaoLogin?.addEventListener('click', () => handleOAuthLogin('kakao'));
    btnGoogleLogin?.addEventListener('click', () => handleOAuthLogin('google'));
    btnLogout?.addEventListener('click', () => {
        releaseQueueIfWaiting({ silent: true }).finally(() => {
            disconnectMatchWs();
            BluffBallAuth.logout();
            renderLoginState();
        });
    });
    btnModeSingle?.addEventListener('click', () => startSingleModeMatching());
    btnCancelMatch?.addEventListener('click', () => cancelMatching());

    window.addEventListener('pagehide', releaseQueueOnPageHide);

    renderLoginState();
    if (!BluffBallAuth.isLoggedIn()) {
        showOAuthSetupHint();
    }
})();
