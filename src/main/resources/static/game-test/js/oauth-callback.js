(() => {
    const statusEl = document.getElementById('callbackStatus');
    const errorEl = document.getElementById('callbackError');

    function showError(message) {
        if (statusEl) {
            statusEl.textContent = '로그인에 실패했습니다.';
        }
        if (errorEl) {
            errorEl.textContent = message;
            errorEl.hidden = false;
        }
    }

    (async () => {
        const params = new URLSearchParams(window.location.search);
        const code = params.get('code');
        const oauthError = params.get('error');
        const provider = sessionStorage.getItem(BluffBallAuth.OAUTH_PROVIDER_KEY);

        if (oauthError) {
            showError(`소셜 로그인 취소 또는 오류: ${oauthError}`);
            return;
        }

        if (!code) {
            showError('인가 코드(code)가 URL에 없습니다.');
            return;
        }

        if (!provider) {
            showError('로그인 provider 정보가 없습니다. 홈에서 다시 시도해 주세요.');
            return;
        }

        try {
            const config = await BluffBallAuth.fetchOAuthConfig();
            const tokens = await BluffBallAuth.exchangeCodeForTokens(provider, code, config.redirectUri);
            BluffBallAuth.saveTokens(tokens);
            sessionStorage.removeItem(BluffBallAuth.OAUTH_PROVIDER_KEY);
            window.location.replace('/game-test/Home.html');
        } catch (e) {
            showError(e.message || String(e));
        }
    })();
})();
