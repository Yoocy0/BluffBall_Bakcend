(() => {
    const HOME = '/game-test/Home.html';
    const STORAGE_SETUP_NUMBERS = 'bluffball.mySetupNumbers';
    const STORAGE_DOUBLE_JUDGMENT = 'bluffball.doubleJudgment';
    const STORAGE_GAME_MODE = 'bluffball.gameMode';
    const STORAGE_PITCH_HAND = 'bluffball.pitchHand';
    const STORAGE_TEAMMATE_BOTS = 'bluffball.teammateBotIds';
    const STORAGE_OPPONENT_BOTS = 'bluffball.opponentBotIds';

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
        if (isLeagueMode()) {
            window.BluffBallPitcherSub?.mount?.({
                matchSessionId: getMatchSessionId(),
                pollMs: options.pitcherSubPollMs ?? 2500,
            });
        }
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

    function getGameMode() {
        return sessionStorage.getItem(STORAGE_GAME_MODE) || '';
    }

    /** 쇼다운/커스텀/봇전만 인게임 드로우·멀리건 */
    function usesInGameMulligan(gameMode) {
        const mode = gameMode || getGameMode();
        return mode === 'SHOWDOWN' || mode === 'CUSTOM' || mode === 'BOT';
    }

    function isLeagueMode(gameMode) {
        const mode = gameMode || getGameMode();
        return mode === 'COMPACT_LEAGUE' || mode === 'FULL_LEAGUE';
    }

    async function fetchSessionState(matchSessionId) {
        const id = matchSessionId || getMatchSessionId();
        const token = window.BluffBallAuth?.getAccessToken?.();
        if (!id || !token) {
            return null;
        }
        const res = await fetch(`/api/v1/game/${encodeURIComponent(id)}/state`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        if (!res.ok) {
            return null;
        }
        return res.json();
    }

    /**
     * 세션 스냅샷으로 역할·모드·핸드·멀리건 플래그를 맞춘다.
     * 리그는 인게임 멀리건이 없으므로 play-ready로 표시한다.
     */
    function applySessionState(session) {
        if (!session) {
            return;
        }
        if (session.matchSessionId) {
            sessionStorage.setItem('bluffball.matchSessionId', session.matchSessionId);
        }
        if (session.gameMode) {
            sessionStorage.setItem(STORAGE_GAME_MODE, session.gameMode);
        }
        if (session.pitcherUserId != null) {
            window.BluffBallRole?.syncRoleFromPitcherUserId?.(session.pitcherUserId);
        } else if (session.myRole === 'PITCHER') {
            window.BluffBallRole?.setRole?.('pitcher');
        } else if (session.myRole === 'BATTER') {
            window.BluffBallRole?.setRole?.('batter');
        }
        if (session.myCardHand?.length) {
            sessionStorage.setItem(STORAGE_PITCH_HAND, JSON.stringify(session.myCardHand));
        }
        if (!usesInGameMulligan(session.gameMode) || session.allMulliganDone) {
            window.BluffBallGameWs?.setAllMulliganReady?.(true);
            window.BluffBallGameWs?.setMyMulliganDone?.(true);
        }
    }

    function readIdList(key) {
        try {
            const raw = sessionStorage.getItem(key);
            if (!raw) {
                return [];
            }
            const parsed = JSON.parse(raw);
            return Array.isArray(parsed) ? parsed.map(Number).filter((n) => !Number.isNaN(n)) : [];
        } catch (_) {
            return [];
        }
    }

    function saveBotSides({ teammateBotIds, opponentBotIds }) {
        sessionStorage.setItem(
            STORAGE_TEAMMATE_BOTS,
            JSON.stringify((teammateBotIds || []).map(Number)),
        );
        sessionStorage.setItem(
            STORAGE_OPPONENT_BOTS,
            JSON.stringify((opponentBotIds || []).map(Number)),
        );
    }

    function getTeammateBotIds() {
        return readIdList(STORAGE_TEAMMATE_BOTS);
    }

    function getOpponentBotIds() {
        return readIdList(STORAGE_OPPONENT_BOTS);
    }

    /** @returns {'me'|'teammate'|'opponent'|'unknown'} */
    function classifyActor(userId) {
        if (userId == null) {
            return 'unknown';
        }
        const id = Number(userId);
        const myId = window.BluffBallRole?.getMyUserId?.();
        if (myId != null && Number(myId) === id) {
            return 'me';
        }
        if (getTeammateBotIds().includes(id)) {
            return 'teammate';
        }
        if (getOpponentBotIds().includes(id)) {
            return 'opponent';
        }
        return 'unknown';
    }

    function describeActor(userId) {
        const kind = classifyActor(userId);
        const labels = {
            me: '나',
            teammate: '우리 팀 봇',
            opponent: '상대 봇',
            unknown: '플레이어',
        };
        return { kind, label: labels[kind] || '플레이어' };
    }

    /**
     * 세션 기준으로 다음 화면을 정한다.
     * 사람 행동 → 조작 화면 / 아군·상대 봇 → 관전 화면
     */
    function routeBySession(session) {
        if (!session) {
            return { page: null, reason: 'no-session' };
        }
        if (session.board?.gameOver || session.phase === 'ENDED') {
            return { page: '/game-test/GameEnd.html', reason: 'ended' };
        }
        if (!session.setupComplete || session.phase === 'SETUP_NUMBERS') {
            return { page: '/game-test/SetupNumber.html', reason: 'setup' };
        }

        const turn = session.turn || {};
        const pitcherKind = classifyActor(session.pitcherUserId);
        const batterKind = classifyActor(session.batterUserId);

        if (!turn.pitcherSelectionComplete) {
            if (pitcherKind === 'me') {
                return { page: '/game-test/PitcherSelect.html', reason: 'my-pitch' };
            }
            return { page: '/game-test/BotSpectate.html', reason: 'watch-pitcher' };
        }

        if (!turn.batterSelectionComplete) {
            if (batterKind === 'me') {
                if (turn.startCoordinateNumber > 0) {
                    sessionStorage.setItem(
                        'bluffball.startCoordinate',
                        String(turn.startCoordinateNumber),
                    );
                    return { page: '/game-test/BatterCoordSelect.html', reason: 'my-swing' };
                }
                return { page: '/game-test/BatterWait.html', reason: 'my-batter-wait' };
            }
            return { page: '/game-test/BotSpectate.html', reason: 'watch-batter' };
        }

        return { page: '/game-test/BatterResult.html', reason: 'result' };
    }

    function goToPlayAfterReady(matchSessionId) {
        const id = matchSessionId || getMatchSessionId();
        fetchSessionState(id).then((session) => {
            if (session) {
                applySessionState(session);
                const decision = routeBySession(session);
                if (decision.page) {
                    goWithMatch(decision.page, id);
                    return;
                }
            }
            if (window.BluffBallRole?.isPitcher?.()) {
                goWithMatch('/game-test/PitcherSelect.html', id);
            } else {
                goWithMatch('/game-test/BatterWait.html', id);
            }
        }).catch(() => {
            goWithMatch('/game-test/BatterWait.html', id);
        });
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
        // 같은 페이지로 반복 이동하면 WS가 끊겼다 붙는다.
        if (window.location.pathname === path) {
            return;
        }
        const qs = id ? `?matchSessionId=${encodeURIComponent(id)}` : '';
        go(`${path}${qs}`);
    }

    /**
     * 짧은 SockJS 재연결 flicker를 배너에 바로 노출하지 않는다.
     * @param {number} [graceMs=2500]
     */
    function createConnectionTracker(graceMs = 2500) {
        let disconnectedSince = null;
        return {
            noteConnected() {
                disconnectedSince = null;
            },
            noteDisconnected() {
                if (disconnectedSince == null) {
                    disconnectedSince = Date.now();
                }
            },
            get() {
                if (window.BluffBallGameWs?.isConnected?.()) {
                    disconnectedSince = null;
                    return true;
                }
                if (disconnectedSince == null) {
                    disconnectedSince = Date.now();
                }
                if (Date.now() - disconnectedSince < graceMs) {
                    return null;
                }
                return false;
            },
        };
    }

    async function fetchGameStatus(matchSessionId) {
        const id = matchSessionId || getMatchSessionId();
        // 세션 보드가 실시간 이닝·스코어의 정본. WS 캐시는 마지막 판정 순간에 멈출 수 있다.
        try {
            const session = await fetchSessionState(id);
            const board = session?.board;
            if (board && board.inning != null) {
                return {
                    inning: board.inning,
                    isTop: board.isTop,
                    homeScore: board.homeScore,
                    awayScore: board.awayScore,
                    balls: board.balls,
                    strikes: board.strikes,
                    outs: board.outs,
                    firstBase: board.firstBase,
                    secondBase: board.secondBase,
                    thirdBase: board.thirdBase,
                    totalInnings: board.totalInnings,
                };
            }
        } catch (_) {
            /* fall through */
        }

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

    /**
     * 화면 상단 턴/연결 배너.
     * @param {{ tone?: 'mine'|'wait'|'info', title: string, sub?: string, connected?: boolean|null }} state
     */
    function setTurnBanner(state) {
        let el = document.getElementById('turnBanner');
        if (!el) {
            el = document.createElement('div');
            el.id = 'turnBanner';
            el.className = 'turn-banner';
            document.body.prepend(el);
        }
        const tone = state?.tone || 'info';
        el.className = `turn-banner turn-banner-${tone}`;
        const conn = state?.connected;
        let connHtml = '';
        if (conn === true) {
            connHtml = '<span class="turn-banner-conn turn-banner-conn-ok">연결됨</span>';
        } else if (conn === false) {
            connHtml = '<span class="turn-banner-conn turn-banner-conn-bad">연결 끊김 · 재연결 중</span>';
        }
        const title = state?.title || '';
        const sub = state?.sub || '';
        el.innerHTML = `
            <div class="turn-banner-main">
                <p class="turn-banner-title"></p>
                <p class="turn-banner-sub" hidden></p>
            </div>
            ${connHtml}
        `;
        el.querySelector('.turn-banner-title').textContent = title;
        const subEl = el.querySelector('.turn-banner-sub');
        if (sub) {
            subEl.textContent = sub;
            subEl.hidden = false;
        }
        el.hidden = !title;
    }

    function clearTurnBanner() {
        const el = document.getElementById('turnBanner');
        if (el) {
            el.hidden = true;
        }
    }

    /**
     * 세션 스냅샷으로 내 차례/대기 배너를 갱신한다.
     * @param {object|null} session
     * @param {{ connected?: boolean|null }} opts
     */
    function refreshTurnBannerFromSession(session, opts = {}) {
        const connected = opts.connected;
        if (!session) {
            setTurnBanner({
                tone: 'info',
                title: '상태 확인 중…',
                sub: '서버에서 턴 정보를 불러오는 중입니다.',
                connected,
            });
            return;
        }

        const turn = session.turn || {};
        const pitcher = describeActor(session.pitcherUserId);
        const batter = describeActor(session.batterUserId);

        if (session.board?.gameOver || session.phase === 'ENDED') {
            setTurnBanner({ tone: 'info', title: '경기 종료', connected });
            return;
        }

        if (!session.setupComplete || session.phase === 'SETUP_NUMBERS') {
            setTurnBanner({
                tone: session.mySetupComplete ? 'wait' : 'mine',
                title: session.mySetupComplete ? '대기 — 다른 플레이어 셋업 중' : '내 차례 — 블러핑 숫자 제출',
                sub: '역할에 맞는 숫자만 고르면 됩니다.',
                connected,
            });
            return;
        }

        if (!turn.pitcherSelectionComplete) {
            if (pitcher.kind === 'me') {
                setTurnBanner({
                    tone: 'mine',
                    title: '내 차례 — 투수: 구종·좌표를 선택하세요',
                    sub: '선택하면 타자에게 시작 좌표가 공개됩니다.',
                    connected,
                });
            } else if (pitcher.kind === 'teammate') {
                setTurnBanner({
                    tone: 'spectate',
                    title: '관전 — 우리 팀 투수 봇',
                    sub: '아군 투수 봇 화면을 따라갑니다.',
                    connected,
                });
            } else {
                setTurnBanner({
                    tone: 'wait',
                    title: '관전/대기 — 상대 투수 봇',
                    sub: '상대 투수가 구종을 고르는 중입니다.',
                    connected,
                });
            }
            return;
        }

        if (!turn.batterSelectionComplete) {
            if (batter.kind === 'me') {
                setTurnBanner({
                    tone: 'mine',
                    title: '내 차례 — 타자: 좌표·타이밍을 선택하세요',
                    sub: '5초 안에 선택하지 않으면 스윙 미발동(스트라이크)입니다.',
                    connected,
                });
            } else if (batter.kind === 'teammate') {
                setTurnBanner({
                    tone: 'spectate',
                    title: '관전 — 우리 팀 타자 봇',
                    sub: '아군 타자 봇 타격을 이 화면에서 확인합니다.',
                    connected,
                });
            } else {
                setTurnBanner({
                    tone: 'wait',
                    title: '관전/대기 — 상대 타자 봇',
                    sub: '상대 타자가 타격하는 중입니다.',
                    connected,
                });
            }
            return;
        }

        setTurnBanner({
            tone: 'info',
            title: '턴 결과 처리 중…',
            connected,
        });
    }

    async function pollTurnBanner(matchSessionId, getConnected) {
        const id = matchSessionId || getMatchSessionId();
        if (!id) {
            return null;
        }
        const session = await fetchSessionState(id);
        refreshTurnBannerFromSession(session, {
            connected: typeof getConnected === 'function' ? getConnected() : null,
        });
        const hud = document.getElementById('broadcastHud');
        const board = session?.board;
        if (hud && board && board.inning != null) {
            renderBroadcastHud(hud, {
                inning: board.inning,
                isTop: board.isTop,
                homeScore: board.homeScore,
                awayScore: board.awayScore,
                balls: board.balls,
                strikes: board.strikes,
                outs: board.outs,
                firstBase: board.firstBase,
                secondBase: board.secondBase,
                thirdBase: board.thirdBase,
            });
        }
        return session;
    }

    guardRefreshToHome();

    window.BluffBallNav = {
        HOME,
        guardRefreshToHome,
        getMatchSessionId,
        getGameMode,
        usesInGameMulligan,
        isLeagueMode,
        fetchSessionState,
        applySessionState,
        goToPlayAfterReady,
        saveBotSides,
        getTeammateBotIds,
        getOpponentBotIds,
        classifyActor,
        describeActor,
        routeBySession,
        go,
        goWithMatch,
        createConnectionTracker,
        fetchGameStatus,
        renderBroadcastHud,
        mountBroadcastHud,
        mountSetupNumbersHud,
        mountInGameHud,
        setTurnBanner,
        clearTurnBanner,
        refreshTurnBannerFromSession,
        pollTurnBanner,
        persistMySetupNumbers,
        getStoredMySetupNumbers,
        formatInning,
    };
})();
