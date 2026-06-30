(() => {
    const loginScreen = document.getElementById('loginScreen');
    const homeScreen = document.getElementById('homeScreen');
    const btnKakaoLogin = document.getElementById('btnKakaoLogin');
    const btnGoogleLogin = document.getElementById('btnGoogleLogin');
    const loginError = document.getElementById('loginError');
    const userInfo = document.getElementById('userInfo');
    const btnStart = document.getElementById('btnStart');
    const btnLogout = document.getElementById('btnLogout');
    const startError = document.getElementById('startError');

    /** 로그인 상태에 따라 화면 전환 */
    function renderScreen() {
        const loggedIn = BluffBallAuth.isLoggedIn();

        if (loginScreen) {
            loginScreen.hidden = loggedIn;
        }
        if (homeScreen) {
            homeScreen.hidden = !loggedIn;
        }

        if (loggedIn && userInfo) {
            const userId = BluffBallAuth.getUserIdFromToken();
            userInfo.textContent = userId ? `로그인됨 · userId ${userId}` : '로그인됨';
        }
    }

    /** 로그인 버튼 공통 처리 */
    async function handleOAuthLogin(provider) {
        if (loginError) {
            loginError.hidden = true;
        }
        btnKakaoLogin.disabled = true;
        btnGoogleLogin.disabled = true;

        try {
            await BluffBallAuth.startOAuthLogin(provider);
        } catch (e) {
            if (loginError) {
                loginError.textContent = e.message || String(e);
                loginError.hidden = false;
            }
            btnKakaoLogin.disabled = false;
            btnGoogleLogin.disabled = false;
        }
    }

    /** 화면 진입 시 OAuth 키 설정 상태 표시 */
    async function showOAuthSetupHint() {
        try {
            const config = await BluffBallAuth.fetchOAuthConfig();
            if (btnKakaoLogin) {
                btnKakaoLogin.disabled = !config.kakaoConfigured;
            }
            if (btnGoogleLogin) {
                btnGoogleLogin.disabled = !config.googleConfigured;
            }
            if (!config.kakaoConfigured && !config.googleConfigured && loginError) {
                loginError.textContent =
                    'OAuth 키가 서버에 설정되지 않았습니다. '
                    + 'src/main/resources/oauth-local.example.yml → oauth-local.yml 복사 후 '
                    + 'Client ID/Secret을 입력하고 서버를 재시작하세요.';
                loginError.hidden = false;
            }
        } catch (e) {
            if (loginError) {
                loginError.textContent = `OAuth 설정 확인 실패: ${e.message}`;
                loginError.hidden = false;
            }
        }
    }

    btnKakaoLogin?.addEventListener('click', () => handleOAuthLogin('kakao'));
    btnGoogleLogin?.addEventListener('click', () => handleOAuthLogin('google'));

    btnLogout?.addEventListener('click', () => {
        BluffBallAuth.logout();
        renderScreen();
    });

    btnStart?.addEventListener('click', async () => {
        btnStart.disabled = true;
        if (startError) {
            startError.hidden = true;
        }

        try {
            const res = await fetch('/game-test/api/match', { method: 'POST' });
            if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
            }
            const data = await res.json();

            // 게임 세션만 초기화 (로그인 토큰은 localStorage에 유지)
            Object.keys(sessionStorage).forEach((key) => {
                if (key.startsWith('bluffball.') && key !== BluffBallAuth.OAUTH_PROVIDER_KEY) {
                    sessionStorage.removeItem(key);
                }
            });

            sessionStorage.setItem('bluffball.matchSessionId', data.matchSessionId);
            window.location.href =
                `/game-test/SetupNumber.html?matchSessionId=${encodeURIComponent(data.matchSessionId)}`;
        } catch (e) {
            if (startError) {
                startError.textContent = `게임 시작 실패: ${e.message}`;
                startError.hidden = false;
            }
            btnStart.disabled = false;
        }
    });

    renderScreen();
    if (!BluffBallAuth.isLoggedIn()) {
        showOAuthSetupHint();
    }
})();
