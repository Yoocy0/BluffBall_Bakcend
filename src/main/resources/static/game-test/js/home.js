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
    const btnModeBotCompact = document.getElementById('btnModeBotCompact');
    const btnModeBotFull = document.getElementById('btnModeBotFull');
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
        /** 매칭 성사 시 auto-play에 넘길 봇 ID (상대 봇 매칭 전용) */
        pendingBotUserIds: null,
        pendingTeammateBotIds: null,
        pendingOpponentBotIds: null,
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

    function clearMatchLocalState() {
        sessionStorage.removeItem('bluffball.mulliganDone');
        sessionStorage.removeItem('bluffball.myMulliganDone');
        sessionStorage.removeItem('bluffball.allMulliganReady');
        sessionStorage.removeItem('bluffball.pitcherUserId');
        sessionStorage.removeItem('bluffball.pitchHand');
        sessionStorage.removeItem('bluffball.batterResult');
        sessionStorage.removeItem('bluffball.gameEnd');
        sessionStorage.removeItem('bluffball.mySetupNumbers');
        sessionStorage.removeItem('bluffball.doubleJudgment');
        sessionStorage.removeItem('bluffball.gameMode');
        sessionStorage.removeItem('bluffball.role');
        sessionStorage.removeItem('bluffball.teammateBotIds');
        sessionStorage.removeItem('bluffball.opponentBotIds');
    }

    function navigateToSetup(matchSessionId) {
        state.waitingInQueue = false;
        sessionStorage.setItem('bluffball.matchSessionId', matchSessionId);
        window.location.href =
            `/game-test/SetupNumber.html?matchSessionId=${encodeURIComponent(matchSessionId)}`;
    }

    async function prepareMatchSession(matchSessionId) {
        clearMatchLocalState();
        sessionStorage.setItem('bluffball.matchSessionId', matchSessionId);
        try {
            const session = await BluffBallNav.fetchSessionState(matchSessionId);
            if (session) {
                BluffBallNav.applySessionState(session);
            }
        } catch (_) {
            /* SetupNumber에서 재조회 */
        }
    }

    async function startBotAutoPlay(matchSessionId, botUserIds) {
        if (!matchSessionId || !botUserIds?.length) {
            return;
        }
        const res = await fetch('/game-test/api/bots/league/auto-play', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                matchSessionId,
                botUserIds: botUserIds.map(Number),
            }),
        });
        if (!res.ok) {
            const body = await res.json().catch(() => ({}));
            throw new Error(body.message || `봇 자동 플레이 시작 실패 (HTTP ${res.status})`);
        }
        return res.json();
    }

    function handleMatchComplete(matchSessionId, roleHint) {
        if (state.matchHandled) {
            return;
        }
        state.matchHandled = true;
        state.waitingInQueue = false;

        // 큐 순서 역할 힌트는 쇼다운용. 리그는 prepareMatchSession이 덮어쓴다.
        if (roleHint) {
            BluffBallRole.setRole(roleHint);
        }

        stopMatchingTimer();
        disconnectMatchWs();

        const botIds = state.pendingBotUserIds;
        state.pendingBotUserIds = null;
        if (botIds?.length) {
            startBotAutoPlay(matchSessionId, botIds).catch((e) => {
                console.warn('bot auto-play failed', e);
            });
        }

        matchCompleteModal.hidden = false;
        let remaining = MATCH_COMPLETE_DELAY_SEC;
        matchCompleteCountdown.textContent = String(remaining);

        prepareMatchSession(matchSessionId).finally(() => {
            if (state.pendingTeammateBotIds || state.pendingOpponentBotIds) {
                BluffBallNav.saveBotSides({
                    teammateBotIds: state.pendingTeammateBotIds || [],
                    opponentBotIds: state.pendingOpponentBotIds || [],
                });
            }
            const countdownInterval = setInterval(() => {
                remaining -= 1;
                if (remaining <= 0) {
                    clearInterval(countdownInterval);
                    navigateToSetup(matchSessionId);
                    return;
                }
                matchCompleteCountdown.textContent = String(remaining);
            }, 1000);
        });
    }

    async function parseApiError(res) {
        const body = await res.json().catch(() => ({}));
        return body.message || `요청 실패 (HTTP ${res.status})`;
    }

    /** 아군 로스터를 포맷에 맞게 봇으로 채운다. */
    async function fillMyRoster(format) {
        const leaderUserId = BluffBallAuth.getUserIdFromToken();
        if (!leaderUserId) {
            throw new Error('로그인 정보가 없습니다.');
        }
        const res = await fetch('/game-test/api/bots/league/fill-roster', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                leaderUserId: Number(leaderUserId),
                teamId: null,
                format,
                myRole: 'BATTER',
            }),
        });
        if (!res.ok) {
            throw new Error(await parseApiError(res));
        }
        return res.json();
    }

    /** 상대 봇 팀을 만들고 같은 티어 큐에 넣는다. */
    async function enqueueOpponentBot(format, tier) {
        const res = await fetch('/game-test/api/bots/league/enqueue-opponent', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ format, tier }),
        });
        if (!res.ok) {
            throw new Error(await parseApiError(res));
        }
        return res.json();
    }

    /**
     * 사람(리더)을 제외한 아군 로스터 ID.
     * Compact: 타순 + 전담 투수 / Full: 타순(선발 포함)
     */
    function collectTeammateBotIds(filled, humanUserId) {
        const human = Number(humanUserId);
        const ids = new Set();
        (filled.batterUserIds || []).forEach((id) => ids.add(Number(id)));
        if (filled.pitcherUserId != null) {
            ids.add(Number(filled.pitcherUserId));
        }
        ids.delete(human);
        return [...ids];
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
        state.pendingBotUserIds = null;
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
            state.pendingBotUserIds = null;
            showScreen('modes');
            showError(homeError, e.message || String(e));
        }
    }

    /**
     * 아군 봇 충원 → 상대 봇 큐잉 → 내 팀 큐 진입 → auto-play.
     */
    async function startBotOpponentMatching(format, label) {
        showError(homeError, '');
        showError(matchingError, '');
        state.matchHandled = false;
        state.waitingInQueue = false;
        state.pendingBotUserIds = null;
        state.queueKind = 'LEAGUE';
        state.leagueFormat = format;
        matchingModeLabel.textContent = `${label} 준비 중...`;

        showScreen('matching');
        startMatchingTimer();

        const humanUserId = BluffBallAuth.getUserIdFromToken();
        try {
            matchingModeLabel.textContent = `${label} · 아군 봇 채우는 중...`;
            const filled = await fillMyRoster(format);
            const teammateBots = collectTeammateBotIds(filled, humanUserId);

            matchingModeLabel.textContent = `${label} · 상대 봇 대기열 등록 중...`;
            const progress = await ensureLeagueTier(format);
            const opponent = await enqueueOpponentBot(format, progress.currentTier);

            const opponentBots = (opponent.botUserIds || []).map(Number);
            const allBots = new Set([...teammateBots, ...opponentBots]);
            allBots.delete(Number(humanUserId));
            state.pendingBotUserIds = [...allBots];
            state.pendingTeammateBotIds = teammateBots;
            state.pendingOpponentBotIds = opponentBots;

            matchingModeLabel.textContent = `${label} 매칭 중...`;
            const result = await joinMatchQueue();
            if (result === 'waiting') {
                state.waitingInQueue = true;
                connectMatchNotificationWs();
            }
        } catch (e) {
            stopMatchingTimer();
            disconnectMatchWs();
            state.waitingInQueue = false;
            state.pendingBotUserIds = null;
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
        state.pendingBotUserIds = null;
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
    btnModeBotCompact?.addEventListener('click', () => startBotOpponentMatching('COMPACT', '컴팩트 · 상대 봇'));
    btnModeBotFull?.addEventListener('click', () => startBotOpponentMatching('FULL', '풀 · 상대 봇'));
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
