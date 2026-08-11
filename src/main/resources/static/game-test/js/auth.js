(() => {
    /** localStorage 키 — 게임 sessionStorage clear와 분리 */
    const ACCESS_TOKEN_KEY = 'bluffball.accessToken';
    const REFRESH_TOKEN_KEY = 'bluffball.refreshToken';
    const IS_NEW_USER_KEY = 'bluffball.isNewUser';

    /** OAuth provider 임시 저장 (sessionStorage — redirect 전후 1회용) */
    const OAUTH_PROVIDER_KEY = 'bluffball.oauthProvider';

    const OAUTH_CONFIG_URL = '/game-test/api/oauth-config';

    /** 로그인 여부 */
    function isLoggedIn() {
        return !!localStorage.getItem(ACCESS_TOKEN_KEY);
    }

    /** Access Token 조회 */
    function getAccessToken() {
        return localStorage.getItem(ACCESS_TOKEN_KEY) || '';
    }

    /** Refresh Token 조회 */
    function getRefreshToken() {
        return localStorage.getItem(REFRESH_TOKEN_KEY) || '';
    }

    /** 로그인 API 응답 저장 */
    function saveTokens(loginResponse) {
        localStorage.setItem(ACCESS_TOKEN_KEY, loginResponse.accessToken);
        localStorage.setItem(REFRESH_TOKEN_KEY, loginResponse.refreshToken);
        localStorage.setItem(IS_NEW_USER_KEY, String(!!loginResponse.isNewUser));
    }

    /** 로그아웃 — 토큰 삭제 */
    function logout() {
        localStorage.removeItem(ACCESS_TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY);
        localStorage.removeItem(IS_NEW_USER_KEY);
    }

    /** JWT payload에서 userId(sub) 추출 */
    function getUserIdFromToken() {
        const token = getAccessToken();
        if (!token) {
            return null;
        }
        try {
            const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
            return payload.sub || null;
        } catch (_) {
            return null;
        }
    }

    /** 서버 OAuth 설정 조회 */
    async function fetchOAuthConfig() {
        const res = await fetch(OAUTH_CONFIG_URL);
        if (!res.ok) {
            throw new Error(`OAuth 설정 조회 실패 (HTTP ${res.status})`);
        }
        return res.json();
    }

    const OAUTH_SETUP_HINT =
        '서버 OAuth 키가 없습니다. src/main/resources/oauth-local.example.yml 을 '
        + 'oauth-local.yml 로 복사한 뒤 Client ID/Secret을 입력하고 서버를 재시작하세요.';

    /** 카카오/구글 OAuth authorize URL로 이동 */
    async function startOAuthLogin(provider) {
        const config = await fetchOAuthConfig();

        if (provider === 'kakao' && !config.kakaoConfigured) {
            throw new Error(`카카오 Client ID가 설정되지 않았습니다. ${OAUTH_SETUP_HINT}`);
        }
        if (provider === 'google' && !config.googleConfigured) {
            throw new Error(`구글 Client ID가 설정되지 않았습니다. ${OAUTH_SETUP_HINT}`);
        }

        sessionStorage.setItem(OAUTH_PROVIDER_KEY, provider);

        const redirectUri = provider === 'kakao' ? config.kakaoRedirectUri : config.googleRedirectUri;

        let authorizeUrl;
        if (provider === 'kakao') {
            authorizeUrl = 'https://kauth.kakao.com/oauth/authorize'
                + `?client_id=${encodeURIComponent(config.kakaoClientId)}`
                + `&redirect_uri=${encodeURIComponent(redirectUri)}`
                + '&response_type=code';
        } else if (provider === 'google') {
            const scope = encodeURIComponent('openid email profile');
            authorizeUrl = 'https://accounts.google.com/o/oauth2/v2/auth'
                + `?client_id=${encodeURIComponent(config.googleClientId)}`
                + `&redirect_uri=${encodeURIComponent(redirectUri)}`
                + '&response_type=code'
                + `&scope=${scope}`;
        } else {
            throw new Error(`지원하지 않는 provider: ${provider}`);
        }

        window.location.href = authorizeUrl;
    }

    /** 인가 코드로 백엔드 로그인 API 호출 */
    async function exchangeCodeForTokens(provider, authorizationCode, redirectUri) {
        const res = await fetch(`/api/v1/auth/login/${provider}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ authorizationCode, redirectUri }),
        });

        if (!res.ok) {
            const body = await res.json().catch(() => ({}));
            throw new Error(body.message || `로그인 실패 (HTTP ${res.status})`);
        }

        return res.json();
    }

    /**
     * 개발자 PIN 로그인 — POST /game-test/api/dev-login
     * @param {string} pin 4자리 PIN
     */
    async function devLogin(pin) {
        const res = await fetch('/game-test/api/dev-login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ pin }),
        });
        if (!res.ok) {
            const body = await res.json().catch(() => ({}));
            throw new Error(body.message || `개발자 로그인 실패 (HTTP ${res.status})`);
        }
        return res.json();
    }

    window.BluffBallAuth = {
        ACCESS_TOKEN_KEY,
        REFRESH_TOKEN_KEY,
        OAUTH_PROVIDER_KEY,
        isLoggedIn,
        getAccessToken,
        getRefreshToken,
        saveTokens,
        logout,
        getUserIdFromToken,
        fetchOAuthConfig,
        startOAuthLogin,
        exchangeCodeForTokens,
        devLogin,
    };
})();
