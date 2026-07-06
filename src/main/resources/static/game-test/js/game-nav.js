(() => {
    const HOME = '/game-test/Home.html';
    const STORAGE_SETUP_NUMBERS = 'bluffball.mySetupNumbers';
    const STORAGE_DOUBLE_JUDGMENT = 'bluffball.doubleJudgment';

    function persistMySetupNumbers(numbers) {
        if (!numbers) {
            return;
        }
        sessionStorage.setItem(STORAGE_SETUP_NUMBERS, JSON.stringify(numbers));
    }

    function getStoredMySetupNumbers() {
        try {
            const raw = sessionStorage.getItem(STORAGE_SETUP_NUMBERS);
            return raw ? JSON.parse(raw) : null;
        } catch (_) {
            return null;
        }
    }

    function pickUserMapEntry(map, userId) {
        if (!map || userId == null) {
            return null;
        }
        return map[userId] ?? map[String(userId)] ?? null;
    }

    async function fetchSetupMetaFromServer(matchSessionId) {
        const userId = window.BluffBallAuth?.getUserIdFromToken?.();
        if (!matchSessionId || !userId) {
            return null;
        }

        const res = await fetch(`/game-test/api/match/${encodeURIComponent(matchSessionId)}/setup-numbers`);
        if (!res.ok) {
            return null;
        }

        const data = await res.json();
        const numbers = {
            outNumList: pickUserMapEntry(data.outNumbers, userId) || [],
            dpNumList: pickUserMapEntry(data.dpNumbers, userId) || [],
            tripleNumList: pickUserMapEntry(data.tripleNumbers, userId) || [],
            hrNumList: pickUserMapEntry(data.hrNumbers, userId) || [],
        };

        const hasAny = numbers.outNumList.length
            || numbers.dpNumList.length
            || numbers.tripleNumList.length
            || numbers.hrNumList.length;

        let doubleJudgment = null;
        if (data.doubleJudgmentTargetFace >= 1 && data.doubleJudgmentTargetFace <= 6) {
            doubleJudgment = {
                targetFace: data.doubleJudgmentTargetFace,
                useFrontDice: data.doubleJudgmentUseFrontDice === true,
            };
            sessionStorage.setItem(STORAGE_DOUBLE_JUDGMENT, JSON.stringify(doubleJudgment));
        }

        if (hasAny) {
            persistMySetupNumbers(numbers);
        }

        return { numbers: hasAny ? numbers : null, doubleJudgment };
    }

    async function fetchMySetupNumbers(matchSessionId) {
        const meta = await fetchSetupMetaFromServer(matchSessionId);
        return meta?.numbers ?? null;
    }

    function getStoredDoubleJudgment() {
        try {
            const raw = sessionStorage.getItem(STORAGE_DOUBLE_JUDGMENT);
            return raw ? JSON.parse(raw) : null;
        } catch (_) {
            return null;
        }
    }

    async function resolveDoubleJudgment(matchSessionId) {
        const stored = getStoredDoubleJudgment();
        if (stored?.targetFace) {
            return stored;
        }
        const meta = await fetchSetupMetaFromServer(matchSessionId);
        return meta?.doubleJudgment ?? null;
    }

    async function resolveMySetupNumbers(matchSessionId) {
        const stored = getStoredMySetupNumbers();
        if (stored?.outNumList?.length) {
            return stored;
        }
        return fetchMySetupNumbers(matchSessionId);
    }

    function formatNumberList(list) {
        if (!Array.isArray(list) || list.length === 0) {
            return '-';
        }
        return list.join(' · ');
    }

    function formatDoubleJudgmentLabel(doubleJudgment) {
        if (!doubleJudgment?.targetFace) {
            return '';
        }
        const side = doubleJudgment.useFrontDice ? '앞면' : '뒷면';
        return `${side} - ${doubleJudgment.targetFace}`;
    }

    function renderSetupNumbersHud(container, numbers, doubleJudgment) {
        if (!container) {
            return;
        }
        if (!numbers && !doubleJudgment?.targetFace) {
            container.hidden = true;
            container.innerHTML = '';
            return;
        }

        const numbersBlock = numbers ? `
            <p class="setup-numbers-title">내 셋업 숫자</p>
            <dl class="setup-numbers-list">
                <div class="setup-numbers-row">
                    <dt>아웃</dt>
                    <dd>${formatNumberList(numbers.outNumList)}</dd>
                </div>
                <div class="setup-numbers-row">
                    <dt>병살</dt>
                    <dd>${formatNumberList(numbers.dpNumList)}</dd>
                </div>
                <div class="setup-numbers-row">
                    <dt>3루타</dt>
                    <dd>${formatNumberList(numbers.tripleNumList)}</dd>
                </div>
                <div class="setup-numbers-row">
                    <dt>홈런</dt>
                    <dd>${formatNumberList(numbers.hrNumList)}</dd>
                </div>
            </dl>
        ` : '';

        const doubleLabel = formatDoubleJudgmentLabel(doubleJudgment);
        const doubleBlock = doubleLabel ? `
            <div class="setup-numbers-double">
                <p class="setup-numbers-subtitle">2루타 조건</p>
                <p class="setup-numbers-double-value">${doubleLabel}</p>
            </div>
        ` : '';

        container.hidden = false;
        container.innerHTML = `${numbersBlock}${doubleBlock}`;
    }

    async function mountSetupNumbersHud(containerId) {
        const el = document.getElementById(containerId || 'setupNumbersHud');
        const matchSessionId = getMatchSessionId();
        if (!el || !matchSessionId) {
            return null;
        }

        try {
            const [numbers, doubleJudgment] = await Promise.all([
                resolveMySetupNumbers(matchSessionId),
                resolveDoubleJudgment(matchSessionId),
            ]);
            renderSetupNumbersHud(el, numbers, doubleJudgment);
            return { numbers, doubleJudgment };
        } catch (_) {
            el.hidden = true;
            return null;
        }
    }

    async function mountInGameHud(options = {}) {
        const broadcastStatus = await mountBroadcastHud(options.broadcastContainerId);
        const setupNumbers = await mountSetupNumbersHud(options.setupContainerId);
        return { broadcastStatus, setupNumbers };
    }

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
        const fromWs = BluffBallGameWs?.getStoredTurnResult?.();
        if (fromWs?.inning != null) {
            return {
                inning: fromWs.inning,
                isTop: fromWs.isTop,
                homeScore: fromWs.homeScore,
                awayScore: fromWs.awayScore,
                balls: fromWs.balls,
                strikes: fromWs.strikes,
                outs: fromWs.outs,
                firstBase: fromWs.firstBase,
                secondBase: fromWs.secondBase,
                thirdBase: fromWs.thirdBase,
            };
        }
        throw new Error('스코어 정보 없음');
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
        mountSetupNumbersHud,
        mountInGameHud,
        persistMySetupNumbers,
        getStoredMySetupNumbers,
        formatInning,
    };
})();
