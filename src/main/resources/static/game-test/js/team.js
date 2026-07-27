(() => {
    const formatSelect = document.getElementById('formatSelect');
    const lineupSlots = document.getElementById('lineupSlots');
    const memberPool = document.getElementById('memberPool');
    const teamMeta = document.getElementById('teamMeta');
    const slotHint = document.getElementById('slotHint');
    const pageError = document.getElementById('pageError');
    const pageOk = document.getElementById('pageOk');
    const createTeamBox = document.getElementById('createTeamBox');
    const teamNameInput = document.getElementById('teamNameInput');
    const btnCreateTeam = document.getElementById('btnCreateTeam');
    const btnSaveLineup = document.getElementById('btnSaveLineup');

    const state = {
        team: null,
        members: [],
        /** @type {Array<number|null>} batting order slots */
        batters: [],
        /** @type {number|null} */
        pitcher: null,
    };

    function showError(msg) {
        pageError.textContent = msg || '';
        pageError.hidden = !msg;
        if (msg) {
            pageOk.hidden = true;
        }
    }

    function showOk(msg) {
        pageOk.textContent = msg || '';
        pageOk.hidden = !msg;
        if (msg) {
            pageError.hidden = true;
        }
    }

    function formatConfig(format) {
        if (format === 'FULL') {
            return {
                batterCount: 9,
                dedicatedPitcher: false,
                hint: '풀: 타순 9명 · 선발 투수는 타순 중 1명',
            };
        }
        return {
            batterCount: 3,
            dedicatedPitcher: true,
            hint: '컴팩트: 타순 3명 + 전담 투수 1명',
        };
    }

    function memberLabel(userId) {
        const m = state.members.find((x) => x.userId === userId);
        if (!m) {
            return `user ${userId}`;
        }
        return `${m.nickname || '멤버'} (#${userId})`;
    }

    function assignedIds() {
        const ids = new Set(state.batters.filter((id) => id != null));
        if (state.pitcher != null && formatConfig(formatSelect.value).dedicatedPitcher) {
            ids.add(state.pitcher);
        }
        return ids;
    }

    function onDragStart(e, payload) {
        e.dataTransfer.setData('application/json', JSON.stringify(payload));
        e.dataTransfer.effectAllowed = 'move';
    }

    function parseDrag(e) {
        try {
            return JSON.parse(e.dataTransfer.getData('application/json') || '{}');
        } catch (_) {
            return {};
        }
    }

    function clearSlot(kind, index) {
        if (kind === 'pitcher') {
            state.pitcher = null;
        } else {
            state.batters[index] = null;
        }
        render();
    }

    function placeUser(userId, kind, index) {
        const cfg = formatConfig(formatSelect.value);

        // remove from previous slots
        state.batters = state.batters.map((id) => (id === userId ? null : id));
        if (state.pitcher === userId) {
            state.pitcher = null;
        }

        if (kind === 'pitcher') {
            if (!cfg.dedicatedPitcher) {
                // Full: pitcher must already be in batting order — put them in a free batter slot if needed
                if (!state.batters.includes(userId)) {
                    const empty = state.batters.findIndex((id) => id == null);
                    if (empty < 0) {
                        showError('풀 모드에서는 타순 9명을 먼저 채운 뒤 투수를 지정하세요.');
                        render();
                        return;
                    }
                    state.batters[empty] = userId;
                }
            }
            state.pitcher = userId;
        } else {
            const prev = state.batters[index];
            state.batters[index] = userId;
            if (!cfg.dedicatedPitcher && state.pitcher === prev) {
                state.pitcher = userId;
            }
            if (cfg.dedicatedPitcher && state.pitcher === userId) {
                state.pitcher = null;
            }
        }
        showError('');
        render();
    }

    function renderSlots() {
        const cfg = formatConfig(formatSelect.value);
        slotHint.textContent = cfg.hint;
        lineupSlots.innerHTML = '';

        state.batters.forEach((userId, index) => {
            const slot = document.createElement('div');
            slot.className = 'slot-card';
            slot.dataset.kind = 'batter';
            slot.dataset.index = String(index);

            const label = document.createElement('div');
            label.className = 'slot-label';
            label.textContent = `${index + 1}번`;
            slot.appendChild(label);

            const body = document.createElement('div');
            body.className = 'slot-body';
            body.textContent = userId != null ? memberLabel(userId) : '비움';
            slot.appendChild(body);

            if (userId != null) {
                const clearBtn = document.createElement('button');
                clearBtn.type = 'button';
                clearBtn.className = 'slot-clear';
                clearBtn.textContent = '×';
                clearBtn.addEventListener('click', () => clearSlot('batter', index));
                slot.appendChild(clearBtn);
            }

            slot.addEventListener('dragover', (e) => {
                e.preventDefault();
                slot.classList.add('slot-card-over');
            });
            slot.addEventListener('dragleave', () => slot.classList.remove('slot-card-over'));
            slot.addEventListener('drop', (e) => {
                e.preventDefault();
                slot.classList.remove('slot-card-over');
                const data = parseDrag(e);
                if (data.userId != null) {
                    placeUser(Number(data.userId), 'batter', index);
                }
            });

            lineupSlots.appendChild(slot);
        });

        const pitcherSlot = document.createElement('div');
        pitcherSlot.className = 'slot-card slot-card-plus';
        pitcherSlot.dataset.kind = 'pitcher';

        const pLabel = document.createElement('div');
        pLabel.className = 'slot-label';
        pLabel.textContent = cfg.dedicatedPitcher ? '투수 +1' : '선발 투수';
        pitcherSlot.appendChild(pLabel);

        const pBody = document.createElement('div');
        pBody.className = 'slot-body';
        pBody.textContent = state.pitcher != null ? memberLabel(state.pitcher) : '비움';
        pitcherSlot.appendChild(pBody);

        if (state.pitcher != null) {
            const clearBtn = document.createElement('button');
            clearBtn.type = 'button';
            clearBtn.className = 'slot-clear';
            clearBtn.textContent = '×';
            clearBtn.addEventListener('click', () => clearSlot('pitcher'));
            pitcherSlot.appendChild(clearBtn);
        }

        pitcherSlot.addEventListener('dragover', (e) => {
            e.preventDefault();
            pitcherSlot.classList.add('slot-card-over');
        });
        pitcherSlot.addEventListener('dragleave', () => pitcherSlot.classList.remove('slot-card-over'));
        pitcherSlot.addEventListener('drop', (e) => {
            e.preventDefault();
            pitcherSlot.classList.remove('slot-card-over');
            const data = parseDrag(e);
            if (data.userId != null) {
                placeUser(Number(data.userId), 'pitcher');
            }
        });

        lineupSlots.appendChild(pitcherSlot);
    }

    function renderPool() {
        memberPool.innerHTML = '';
        const used = assignedIds();
        const cfg = formatConfig(formatSelect.value);

        state.members.forEach((m) => {
            const chip = document.createElement('div');
            chip.className = 'drag-chip';
            chip.draggable = true;
            chip.textContent = `${m.nickname || '멤버'} (#${m.userId})${m.role === 'LEADER' ? ' · 리더' : ''}`;

            if (cfg.dedicatedPitcher && used.has(m.userId)) {
                chip.classList.add('drag-chip-used');
            } else if (!cfg.dedicatedPitcher && state.batters.includes(m.userId)) {
                chip.classList.add('drag-chip-used');
            }

            chip.addEventListener('dragstart', (e) => onDragStart(e, { userId: m.userId }));
            memberPool.appendChild(chip);
        });

        if (!state.members.length) {
            memberPool.textContent = '팀원이 없습니다.';
        }
    }

    function render() {
        renderSlots();
        renderPool();
    }

    function resetSlotsForFormat() {
        const cfg = formatConfig(formatSelect.value);
        state.batters = Array.from({ length: cfg.batterCount }, () => null);
        state.pitcher = null;
    }

    async function loadLineupIntoState() {
        resetSlotsForFormat();
        if (!state.team) {
            render();
            return;
        }
        const format = formatSelect.value;
        const lineup = await BluffBallTeamApi.getLineup(state.team.teamId, format);
        if (lineup) {
            const cfg = formatConfig(format);
            for (let i = 0; i < cfg.batterCount; i += 1) {
                state.batters[i] = lineup.userIds?.[i] ?? null;
            }
            state.pitcher = lineup.startingPitcherUserId ?? null;
        }
        render();
    }

    async function loadTeam() {
        showError('');
        showOk('');
        if (!BluffBallAuth.isLoggedIn()) {
            window.location.href = '/game-test/Home.html';
            return;
        }

        state.team = await BluffBallTeamApi.getMyTeam();
        if (!state.team) {
            teamMeta.textContent = '소속 팀이 없습니다. 아래에서 창단하세요.';
            createTeamBox.hidden = false;
            btnSaveLineup.disabled = true;
            resetSlotsForFormat();
            render();
            return;
        }

        createTeamBox.hidden = true;
        btnSaveLineup.disabled = false;
        teamMeta.textContent = `${state.team.name} · teamId ${state.team.teamId}`;
        state.members = await BluffBallTeamApi.getMembers(state.team.teamId);
        await loadLineupIntoState();
    }

    async function save() {
        showError('');
        showOk('');
        if (!state.team) {
            showError('팀이 없습니다.');
            return;
        }

        const cfg = formatConfig(formatSelect.value);
        if (state.batters.some((id) => id == null)) {
            showError(`타순 ${cfg.batterCount}명을 모두 채워주세요.`);
            return;
        }
        if (state.pitcher == null) {
            showError('선발 투수를 지정해주세요.');
            return;
        }
        if (!cfg.dedicatedPitcher && !state.batters.includes(state.pitcher)) {
            showError('풀 모드 선발 투수는 타순에 포함되어야 합니다.');
            return;
        }

        try {
            await BluffBallTeamApi.saveLineup(
                state.team.teamId,
                formatSelect.value,
                state.batters.map(Number),
                Number(state.pitcher),
            );
            showOk('라인업을 저장했습니다.');
        } catch (e) {
            showError(e.message || String(e));
        }
    }

    formatSelect.addEventListener('change', () => {
        loadLineupIntoState().catch((e) => showError(e.message || String(e)));
    });
    btnSaveLineup.addEventListener('click', () => save());
    btnCreateTeam.addEventListener('click', async () => {
        showError('');
        const name = (teamNameInput.value || '').trim();
        if (!name) {
            showError('팀 이름을 입력하세요.');
            return;
        }
        try {
            await BluffBallTeamApi.createTeam(name);
            await loadTeam();
            showOk('팀을 만들었습니다.');
        } catch (e) {
            showError(e.message || String(e));
        }
    });

    loadTeam().catch((e) => showError(e.message || String(e)));
})();
