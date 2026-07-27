(() => {
    const MATCH_COMPLETE_DELAY_SEC = 3;

    const loginScreen = document.getElementById('loginScreen');
    const hubScreen = document.getElementById('hubScreen');
    const homeScreen = document.getElementById('homeScreen');
    const matchingScreen = document.getElementById('matchingScreen');
    const matchCompleteModal = document.getElementById('matchCompleteModal');
    const matchCompleteCountdown = document.getElementById('matchCompleteCountdown');

    const btnKakaoLogin = document.getElementById('btnKakaoLogin');
    const btnGoogleLogin = document.getElementById('btnGoogleLogin');
    const loginError = document.getElementById('loginError');
    const userInfo = document.getElementById('userInfo');
    const hubError = document.getElementById('hubError');
    const homeError = document.getElementById('homeError');
    const btnLogout = document.getElementById('btnLogout');

    const btnOpenGameStart = document.getElementById('btnOpenGameStart');
    const btnOpenTeam = document.getElementById('btnOpenTeam');
    const btnOpenPitchCards = document.getElementById('btnOpenPitchCards');
    const btnBackToHub = document.getElementById('btnBackToHub');

    const btnModeShowdown = document.getElementById('btnModeShowdown');
    const btnModeLeagueCompact = document.getElementById('btnModeLeagueCompact');
    const btnModeLeagueFull = document.getElementById('btnModeLeagueFull');
    const matchingModeLabel = document.getElementById('matchingModeLabel');
    const matchingTimer = document.getElementById('matchingTimer');
    const matchingError = document.getElementById('matchingError');
    const btnCancelMatch = document.getElementById('btnCancelMatch');

    const state = {
        stompClient: null,
        timerInterval: null,
        matchingStartedAt: null,
        matchHandled: false,
        waitingInQueue: false,
        /** 'SHOWDOWN' | 'LEAGUE' */
        queueKind: 'SHOWDOWN',
        leagueFormat: null,
    };

    function showScreen(screen) {
        loginScreen.hidden = screen !== 'login';
        hubScreen.hidden = screen !== 'hub';
        homeScreen.hidden = screen !== 'modes';
        matchingScreen.hidden = screen !== 'matching';
    }

    function renderLoginState() {
        if (BluffBallAuth.isLoggedIn()) {
            showScreen('hub');
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

    function cancelQueueUrl() {
        return state.queueKind === 'LEAGUE'
            ? '/api/v1/league-match/queue/cancel'
            : '/api/v1/match/queue/cancel';
    }

    /** REST — 매칭 큐 취소 요청 */
    async function cancelMatchQueueRequest(keepalive = false) {
        const token = BluffBallWs.requireLoginToken();
        const res = await fetch(cancelQueueUrl(), {
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

    /** 페이지 이탈 시 keepalive로 큐 취소 */
    function releaseQueueOnPageHide() {
        if (!state.waitingInQueue) {
            return;
        }
        const token = window.BluffBallAuth?.getAccessToken?.();
        if (!token) {
            return;
        }
        state.waitingInQueue = false;
        fetch(cancelQueueUrl(), {
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

    async function requestJoinShowdownQueue() {
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

    async function ensureLeagueTier(format) {
        const token = BluffBallWs.requireLoginToken();
        let res = await fetch(`/api/v1/leagues/progress?format=${encodeURIComponent(format)}`, {
            headers: { Authorization: `Bearer ${token}` },
        });

        if (res.status === 404) {
            res = await fetch(`/api/v1/leagues/enter?format=${encodeURIComponent(format)}`, {
                method: 'POST',
                headers: { Authorization: `Bearer ${token}` },
            });
            if (!res.ok) {
                const body = await res.json().catch(() => ({}));
                throw new Error(body.message || `리그 진입 실패 (HTTP ${res.status})`);
            }
            return res.json();
        }

        if (!res.ok) {
            const body = await res.json().catch(() => ({}));
            throw new Error(body.message || `리그 진행 조회 실패 (HTTP ${res.status})`);
        }
        return res.json();
    }

    async function requestJoinLeagueQueue(format, tier) {
        const token = BluffBallWs.requireLoginToken();
        const res = await fetch('/api/v1/league-match/queue/join', {
            method: 'POST',
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ format, tier }),
        });
        const body = await res.json().catch(() => ({}));
        return { res, body };
    }

    async function joinMatchQueue() {
        let result;
        if (state.queueKind === 'LEAGUE') {
            const progress = await ensureLeagueTier(state.leagueFormat);
            result = await requestJoinLeagueQueue(state.leagueFormat, progress.currentTier);
        } else {
            result = await requestJoinShowdownQueue();
        }

        let { res, body } = result;

        if (res.status === 409) {
            await cancelMatchQueueRequest();
            if (state.queueKind === 'LEAGUE') {
                const progress = await ensureLeagueTier(state.leagueFormat);
                ({ res, body } = await requestJoinLeagueQueue(state.leagueFormat, progress.currentTier));
            } else {
                ({ res, body } = await requestJoinShowdownQueue());
            }
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
            showScreen('modes');
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

    async function startMatching(queueKind, leagueFormat, label) {
        showError(homeError, '');
        showError(matchingError, '');
        state.matchHandled = false;
        state.waitingInQueue = false;
        state.queueKind = queueKind;
        state.leagueFormat = leagueFormat || null;
        matchingModeLabel.textContent = `${label} 매칭 중...`;

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
            showScreen('modes');
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
        showScreen('modes');
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

    btnOpenGameStart?.addEventListener('click', () => {
        showError(hubError, '');
        showError(homeError, '');
        showScreen('modes');
    });
    btnBackToHub?.addEventListener('click', () => showScreen('hub'));
    btnOpenTeam?.addEventListener('click', () => {
        window.location.href = '/game-test/Team.html';
    });
    btnOpenPitchCards?.addEventListener('click', () => {
        window.location.href = '/game-test/PitchCards.html';
    });

    btnModeShowdown?.addEventListener('click', () => startMatching('SHOWDOWN', null, '쇼다운'));
    btnModeLeagueCompact?.addEventListener('click', () => startMatching('LEAGUE', 'COMPACT', '리그 컴팩트'));
    btnModeLeagueFull?.addEventListener('click', () => startMatching('LEAGUE', 'FULL', '리그 풀'));
    btnCancelMatch?.addEventListener('click', () => cancelMatching());

    window.addEventListener('pagehide', releaseQueueOnPageHide);

    const urlParams = new URLSearchParams(window.location.search);
    const cbAccessToken = urlParams.get('accessToken');
    const cbRefreshToken = urlParams.get('refreshToken');
    if (cbAccessToken && cbRefreshToken) {
        BluffBallAuth.saveTokens({
            accessToken: cbAccessToken,
            refreshToken: cbRefreshToken,
            isNewUser: urlParams.get('isNewUser') === 'true',
        });
        window.history.replaceState({}, '', '/game-test/Home.html');
    }

    renderLoginState();
    if (!BluffBallAuth.isLoggedIn()) {
        showOAuthSetupHint();
    }
})();
