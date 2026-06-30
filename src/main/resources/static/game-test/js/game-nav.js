(() => {
    const HOME = '/game-test/Home.html';

    function isHomePage() {
        const path = window.location.pathname;
        return path.endsWith('Home.html') || path.endsWith('/game-test/') || path.endsWith('/game-test');
    }

    function guardRefreshToHome() {
        if (isHomePage()) {
            return;
        }
        const nav = performance.getEntriesByType('navigation')[0];
        if (nav?.type === 'reload') {
            sessionStorage.clear();
            window.location.replace(HOME);
        }
    }

    function getMatchSessionId() {
        const params = new URLSearchParams(window.location.search);
        return params.get('matchSessionId')
            || sessionStorage.getItem('bluffball.matchSessionId')
            || '';
    }

    function go(url) {
        window.location.href = url;
    }

    function goWithMatch(path, matchSessionId) {
        if (path.endsWith('Home.html')) {
            go(path);
            return;
        }
        const id = matchSessionId || getMatchSessionId();
        if (id) {
            sessionStorage.setItem('bluffball.matchSessionId', id);
        }
        const qs = id ? `?matchSessionId=${encodeURIComponent(id)}` : '';
        go(`${path}${qs}`);
    }

    async function fetchGameStatus(matchSessionId) {
        const res = await fetch(`/game-test/api/match/${matchSessionId}/game-status`);
        if (!res.ok) {
            throw new Error(`HTTP ${res.status}`);
        }
        return res.json();
    }

    function formatInning(s) {
        return `${s.inning}회 ${s.isTop ? '초' : '말'}`;
    }

    function renderBases(first, second, third) {
        return `
            <div class="broadcast-bases" aria-label="주자">
                <span class="base base-2 ${second ? 'on' : ''}">2</span>
                <span class="base base-3 ${third ? 'on' : ''}">3</span>
                <span class="base base-1 ${first ? 'on' : ''}">1</span>
            </div>
        `;
    }

    function renderBroadcastHud(container, status) {
        if (!container || !status) {
            return;
        }

        container.innerHTML = `
            <div class="broadcast-inning">${formatInning(status)}</div>
            <div class="broadcast-scores">
                <div class="broadcast-team">
                    <span class="broadcast-team-label">AWAY</span>
                    <span class="broadcast-team-score">${status.awayScore}</span>
                </div>
                <div class="broadcast-team broadcast-team-home">
                    <span class="broadcast-team-label">HOME</span>
                    <span class="broadcast-team-score">${status.homeScore}</span>
                </div>
            </div>
            <div class="broadcast-count">
                <span>B <strong>${status.balls}</strong></span>
                <span>S <strong>${status.strikes}</strong></span>
                <span>O <strong>${status.outs}</strong></span>
            </div>
            ${renderBases(status.firstBase, status.secondBase, status.thirdBase)}
        `;
    }

    async function mountBroadcastHud(containerId) {
        const el = document.getElementById(containerId || 'broadcastHud');
        const matchSessionId = getMatchSessionId();
        if (!el || !matchSessionId) {
            return null;
        }

        try {
            const status = await fetchGameStatus(matchSessionId);
            renderBroadcastHud(el, status);
            return status;
        } catch (_) {
            el.innerHTML = '<div class="broadcast-inning">스코어 로드 실패</div>';
            return null;
        }
    }

    guardRefreshToHome();

    window.BluffBallNav = {
        HOME,
        guardRefreshToHome,
        getMatchSessionId,
        go,
        goWithMatch,
        fetchGameStatus,
        renderBroadcastHud,
        mountBroadcastHud,
        formatInning,
    };
})();
