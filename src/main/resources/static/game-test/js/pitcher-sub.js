(() => {
    /**
     * 리그 투수 교체 테스트 UI.
     * Compact (0/1) · Full (0/3) — 수비 중일 때 후보를 고른다.
     */
    const PANEL_ID = 'pitcherSubPanel';

    let lastInfo = null;
    let submitting = false;
    let panelEls = null;

    function describeCandidate(userId) {
        const who = window.BluffBallNav?.describeActor?.(userId);
        if (who?.label) {
            return `${who.label} (#${userId})`;
        }
        return `플레이어 #${userId}`;
    }

    function ensurePanel() {
        if (panelEls) {
            return panelEls;
        }
        let root = document.getElementById(PANEL_ID);
        if (!root) {
            root = document.createElement('div');
            root.id = PANEL_ID;
            root.className = 'pitcher-sub-panel';
            root.hidden = true;
            root.innerHTML = `
                <button type="button" class="pitcher-sub-toggle" id="pitcherSubToggle">투수 교체 (0/0)</button>
                <div class="pitcher-sub-sheet" id="pitcherSubSheet" hidden>
                    <p class="pitcher-sub-title">신임 투수 선택</p>
                    <p class="pitcher-sub-hint">Compact 1회 · Full 3회. 이미 등판한 투수는 제외됩니다.</p>
                    <div class="pitcher-sub-candidates" id="pitcherSubCandidates"></div>
                    <p class="pitcher-sub-status" id="pitcherSubStatus"></p>
                    <button type="button" class="pitcher-sub-cancel" id="pitcherSubCancel">닫기</button>
                </div>
            `;
            document.body.appendChild(root);
        }
        panelEls = {
            root,
            toggle: root.querySelector('#pitcherSubToggle'),
            sheet: root.querySelector('#pitcherSubSheet'),
            candidates: root.querySelector('#pitcherSubCandidates'),
            status: root.querySelector('#pitcherSubStatus'),
            cancel: root.querySelector('#pitcherSubCancel'),
        };
        panelEls.toggle.addEventListener('click', () => {
            const open = panelEls.sheet.hidden;
            panelEls.sheet.hidden = !open;
            if (open) {
                renderCandidates(lastInfo);
            }
        });
        panelEls.cancel.addEventListener('click', () => {
            panelEls.sheet.hidden = true;
        });
        return panelEls;
    }

    function setStatus(text, isError) {
        const els = ensurePanel();
        els.status.textContent = text || '';
        els.status.classList.toggle('is-error', !!isError);
    }

    function updateToggle(info) {
        const els = ensurePanel();
        const used = info?.used ?? 0;
        const max = info?.max ?? 0;
        els.toggle.textContent = `투수 교체 (${used}/${max})`;

        const canShow = max > 0 && info?.myTeamDefending;
        els.root.hidden = !canShow;

        const remaining = max - used;
        const hasCandidates = (info?.candidateUserIds || []).length > 0;
        els.toggle.disabled = submitting || remaining <= 0 || !hasCandidates;
        if (!canShow) {
            els.sheet.hidden = true;
            return;
        }
        if (remaining <= 0) {
            els.toggle.title = '교체 한도에 도달했습니다';
        } else if (!hasCandidates) {
            els.toggle.title = '교체 가능한 후보가 없습니다 (dropCard/이미 등판)';
        } else {
            els.toggle.title = '수비 중 투수를 교체합니다';
        }
    }

    function renderCandidates(info) {
        const els = ensurePanel();
        els.candidates.innerHTML = '';
        const list = info?.candidateUserIds || [];
        if (!list.length) {
            els.candidates.innerHTML = '<p class="pitcher-sub-empty">선택 가능한 후보가 없습니다.</p>';
            return;
        }
        list.forEach((userId) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'pitcher-sub-candidate';
            btn.textContent = describeCandidate(userId);
            btn.disabled = submitting;
            btn.addEventListener('click', () => requestSubstitute(userId));
            els.candidates.appendChild(btn);
        });
    }

    async function requestSubstitute(newPitcherUserId) {
        const matchSessionId = window.BluffBallNav?.getMatchSessionId?.();
        if (!matchSessionId || submitting) {
            return;
        }
        submitting = true;
        updateToggle(lastInfo);
        setStatus(`교체 요청 중… → #${newPitcherUserId}`);
        try {
            const res = await fetch('/game-test/api/bots/league/substitute-pitcher', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    matchSessionId,
                    newPitcherUserId: Number(newPitcherUserId),
                }),
            });
            const body = await res.json().catch(() => ({}));
            if (!res.ok) {
                throw new Error(body.message || `교체 실패 (HTTP ${res.status})`);
            }
            lastInfo = {
                ...(lastInfo || {}),
                used: body.used,
                max: body.max,
                candidateUserIds: (lastInfo?.candidateUserIds || [])
                    .filter((id) => Number(id) !== Number(newPitcherUserId)),
            };
            updateToggle(lastInfo);
            setStatus(`교체 완료 · 신임 #${body.newPitcherUserId} (셋업으로 이동)`);
            window.BluffBallRole?.syncRoleFromPitcherUserId?.(body.newPitcherUserId);
            ensurePanel().sheet.hidden = true;
            window.BluffBallNav?.goWithMatch?.('/game-test/SetupNumber.html', matchSessionId);
        } catch (e) {
            setStatus(e.message || String(e), true);
        } finally {
            submitting = false;
            updateToggle(lastInfo);
        }
    }

    function applyFromSession(session) {
        if (!window.BluffBallNav?.isLeagueMode?.()) {
            const els = ensurePanel();
            els.root.hidden = true;
            return;
        }
        const info = session?.pitcherSubstitution;
        lastInfo = info
            ? {
                used: info.used,
                max: info.max,
                candidateUserIds: info.candidateUserIds || [],
                myTeamDefending: info.myTeamDefending === true,
            }
            : null;
        updateToggle(lastInfo);
        if (!ensurePanel().sheet.hidden) {
            renderCandidates(lastInfo);
        }
    }

    async function refresh(matchSessionId) {
        const id = matchSessionId || window.BluffBallNav?.getMatchSessionId?.();
        if (!id || !window.BluffBallNav?.fetchSessionState) {
            return null;
        }
        const session = await window.BluffBallNav.fetchSessionState(id);
        if (session) {
            window.BluffBallNav.applySessionState?.(session);
            applyFromSession(session);
        }
        return session;
    }

    function handleSubstituted({ event }) {
        if (event?.pitcherUserId != null) {
            window.BluffBallRole?.syncRoleFromPitcherUserId?.(event.pitcherUserId);
        }
        const matchId = window.BluffBallNav?.getMatchSessionId?.();
        if (matchId) {
            refresh(matchId).finally(() => {
                window.BluffBallNav?.goWithMatch?.('/game-test/SetupNumber.html', matchId);
            });
        }
    }

    function mount(options = {}) {
        if (!window.BluffBallNav?.isLeagueMode?.()) {
            return;
        }
        ensurePanel();
        const matchSessionId = options.matchSessionId
            || window.BluffBallNav.getMatchSessionId?.();
        refresh(matchSessionId).catch(() => {});

        if (window.BluffBallGameWs?.on && !mount._bound) {
            window.BluffBallGameWs.on(
                window.BluffBallGameWs.EVENT.PITCHER_SUBSTITUTED,
                handleSubstituted,
            );
            mount._bound = true;
        }

        if (options.pollMs > 0 && matchSessionId) {
            setInterval(() => {
                refresh(matchSessionId).catch(() => {});
            }, options.pollMs);
        }
    }

    window.BluffBallPitcherSub = {
        mount,
        refresh,
        applyFromSession,
    };
})();
