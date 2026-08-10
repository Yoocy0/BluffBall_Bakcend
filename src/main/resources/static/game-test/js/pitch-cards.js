(() => {
    const formatSelect = document.getElementById('formatSelect');
    const memberSelect = document.getElementById('memberSelect');
    const cardSlots = document.getElementById('cardSlots');
    const cardPool = document.getElementById('cardPool');
    const teamMeta = document.getElementById('teamMeta');
    const pageError = document.getElementById('pageError');
    const pageOk = document.getElementById('pageOk');
    const btnSaveCards = document.getElementById('btnSaveCards');

    const state = {
        team: null,
        myUserId: null,
        /** 내 인스턴스 목록 (UserPitchCardResponse) */
        inventory: [],
        rosterUserIds: [],
        memberLabels: new Map(),
        /** userId -> { userPitchCardIds: (number|null)[], dropIndex: number } */
        byUser: new Map(),
        selectedUserId: null,
    };

    function handSize(format) {
        return format === 'FULL' ? 5 : 4;
    }

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

    function isEditingSelf() {
        return state.selectedUserId != null
            && state.myUserId != null
            && Number(state.selectedUserId) === Number(state.myUserId);
    }

    function ensureUserHand(userId) {
        if (!state.byUser.has(userId)) {
            const size = handSize(formatSelect.value);
            state.byUser.set(userId, {
                userPitchCardIds: Array.from({ length: size }, () => null),
                dropIndex: size - 1,
            });
        }
        return state.byUser.get(userId);
    }

    function cardLabel(userPitchCardId) {
        const c = state.inventory.find((x) => x.userPitchCardId === userPitchCardId);
        if (!c) {
            return `instance ${userPitchCardId}`;
        }
        const enh = [];
        if (c.changeAmountEnhanced) {
            enh.push('+Δ');
        }
        if (c.timingEnhancement && c.timingEnhancement !== 'NONE') {
            enh.push(c.timingEnhancement);
        }
        const suffix = enh.length ? ` [${enh.join(',')}]` : '';
        return `${c.name}${suffix} (#${userPitchCardId})`;
    }

    function onDragStart(e, payload) {
        e.dataTransfer.setData('application/json', JSON.stringify(payload));
        e.dataTransfer.effectAllowed = 'copyMove';
    }

    function parseDrag(e) {
        try {
            return JSON.parse(e.dataTransfer.getData('application/json') || '{}');
        } catch (_) {
            return {};
        }
    }

    function placeCard(userPitchCardId, slotIndex) {
        if (!isEditingSelf()) {
            return;
        }
        const hand = ensureUserHand(state.selectedUserId);
        hand.userPitchCardIds = hand.userPitchCardIds.map((id, i) => (
            id === userPitchCardId && i !== slotIndex ? null : id
        ));
        hand.userPitchCardIds[slotIndex] = userPitchCardId;
        renderSlots();
        renderPool();
    }

    function clearSlot(slotIndex) {
        if (!isEditingSelf()) {
            return;
        }
        const hand = ensureUserHand(state.selectedUserId);
        hand.userPitchCardIds[slotIndex] = null;
        renderSlots();
        renderPool();
    }

    function renderSlots() {
        cardSlots.innerHTML = '';
        if (state.selectedUserId == null) {
            cardSlots.textContent = '라인업 멤버가 없습니다. 팀 페이지에서 먼저 저장하세요.';
            return;
        }

        const editable = isEditingSelf();
        const hand = ensureUserHand(state.selectedUserId);
        const size = hand.userPitchCardIds.length;
        hand.userPitchCardIds.forEach((userPitchCardId, index) => {
            const isPlus = index === size - 1;
            const slot = document.createElement('div');
            slot.className = isPlus ? 'slot-card slot-card-plus' : 'slot-card';

            const label = document.createElement('div');
            label.className = 'slot-label';
            label.textContent = isPlus ? '+1 (교체 제외)' : `${index + 1}번`;
            slot.appendChild(label);

            const body = document.createElement('div');
            body.className = 'slot-body';
            body.textContent = userPitchCardId != null
                ? cardLabel(userPitchCardId)
                : (editable ? '비움' : '미선택/타인');
            slot.appendChild(body);

            if (editable && userPitchCardId != null) {
                const clearBtn = document.createElement('button');
                clearBtn.type = 'button';
                clearBtn.className = 'slot-clear';
                clearBtn.textContent = '×';
                clearBtn.addEventListener('click', () => clearSlot(index));
                slot.appendChild(clearBtn);
            }

            if (editable) {
                slot.addEventListener('dragover', (e) => {
                    e.preventDefault();
                    slot.classList.add('slot-card-over');
                });
                slot.addEventListener('dragleave', () => slot.classList.remove('slot-card-over'));
                slot.addEventListener('drop', (e) => {
                    e.preventDefault();
                    slot.classList.remove('slot-card-over');
                    const data = parseDrag(e);
                    if (data.userPitchCardId != null) {
                        placeCard(Number(data.userPitchCardId), index);
                    }
                });
            }

            cardSlots.appendChild(slot);
        });
    }

    function renderPool() {
        cardPool.innerHTML = '';
        if (!isEditingSelf()) {
            cardPool.textContent = '본인 로드아웃만 편집할 수 있습니다. 봇 부하는 fill-roster가 인스턴스로 저장합니다.';
            return;
        }

        const hand = ensureUserHand(state.selectedUserId);
        const used = new Set((hand?.userPitchCardIds || []).filter((id) => id != null));

        state.inventory.forEach((card) => {
            const userPitchCardId = card.userPitchCardId;
            const chip = document.createElement('div');
            chip.className = 'drag-chip drag-chip-card';
            chip.draggable = true;
            if (used.has(userPitchCardId)) {
                chip.classList.add('drag-chip-used');
            }
            const amount = card.effectiveChangeAmount ?? card.baseChangeAmount ?? 0;
            const timing = card.effectiveTiming || card.baseTiming || '-';
            chip.innerHTML = `<strong>${card.name}</strong>`
                + `<span>#${userPitchCardId} · ${card.direction || '-'} ${amount} · ${timing} · cost ${card.cost ?? '-'}</span>`;
            chip.addEventListener('dragstart', (e) => onDragStart(e, { userPitchCardId }));
            cardPool.appendChild(chip);
        });
    }

    function renderMemberSelect() {
        memberSelect.innerHTML = '';
        state.rosterUserIds.forEach((userId) => {
            const opt = document.createElement('option');
            opt.value = String(userId);
            const mine = Number(userId) === Number(state.myUserId) ? ' (나)' : '';
            opt.textContent = `${state.memberLabels.get(userId) || `user ${userId}`}${mine}`;
            memberSelect.appendChild(opt);
        });
        if (state.rosterUserIds.length) {
            if (!state.rosterUserIds.includes(state.selectedUserId)) {
                state.selectedUserId = state.rosterUserIds.includes(state.myUserId)
                    ? state.myUserId
                    : state.rosterUserIds[0];
            }
            memberSelect.value = String(state.selectedUserId);
        } else {
            state.selectedUserId = null;
        }
        btnSaveCards.disabled = !state.team || !isEditingSelf();
    }

    function render() {
        renderMemberSelect();
        renderSlots();
        renderPool();
    }

    async function loadForFormat() {
        showError('');
        showOk('');
        state.byUser = new Map();
        state.rosterUserIds = [];

        if (!state.team) {
            render();
            return;
        }

        const format = formatSelect.value;
        const lineup = await BluffBallTeamApi.getLineup(state.team.teamId, format);
        if (!lineup) {
            showError('이 포맷의 라인업이 없습니다. 팀 페이지에서 먼저 저장하세요.');
            render();
            return;
        }

        const roster = [...(lineup.userIds || [])];
        if (lineup.startingPitcherUserId != null && !roster.includes(lineup.startingPitcherUserId)) {
            roster.push(lineup.startingPitcherUserId);
        }
        state.rosterUserIds = roster;

        const size = handSize(format);
        roster.forEach((userId) => {
            state.byUser.set(userId, {
                userPitchCardIds: Array.from({ length: size }, () => null),
                dropIndex: size - 1,
            });
        });

        const saved = await BluffBallTeamApi.getPitchCards(state.team.teamId, format);
        if (saved?.selections) {
            saved.selections.forEach((sel) => {
                const ids = [...(sel.userPitchCardIds || sel.cardIds || [])];
                while (ids.length < size) {
                    ids.push(null);
                }
                if (sel.dropCardId != null) {
                    const withoutDrop = ids.filter((id) => id !== sel.dropCardId);
                    const arranged = [...withoutDrop.slice(0, size - 1)];
                    while (arranged.length < size - 1) {
                        arranged.push(null);
                    }
                    arranged.push(sel.dropCardId);
                    state.byUser.set(sel.userId, { userPitchCardIds: arranged, dropIndex: size - 1 });
                } else {
                    state.byUser.set(sel.userId, {
                        userPitchCardIds: ids.slice(0, size),
                        dropIndex: size - 1,
                    });
                }
            });
        }

        state.selectedUserId = roster.includes(state.myUserId) ? state.myUserId : (roster[0] ?? null);
        render();
    }

    async function loadPage() {
        if (!BluffBallAuth.isLoggedIn()) {
            window.location.href = '/game-test/Home.html';
            return;
        }

        state.myUserId = BluffBallAuth.getUserIdFromToken();
        state.team = await BluffBallTeamApi.getMyTeam();
        if (!state.team) {
            teamMeta.textContent = '소속 팀이 없습니다. 팀 페이지에서 창단하세요.';
            btnSaveCards.disabled = true;
            render();
            return;
        }

        teamMeta.textContent = `${state.team.name} · teamId ${state.team.teamId}`;

        const members = await BluffBallTeamApi.getMembers(state.team.teamId);
        state.memberLabels = new Map(
            members.map((m) => [m.userId, `${m.nickname || '멤버'} (#${m.userId})`]),
        );

        state.inventory = await BluffBallCards.fetchMyPitchCards();
        await loadForFormat();
    }

    async function save() {
        showError('');
        showOk('');
        if (!state.team) {
            showError('팀이 없습니다.');
            return;
        }
        if (!isEditingSelf()) {
            showError('본인 로드아웃만 저장할 수 있습니다.');
            return;
        }

        const hand = ensureUserHand(state.myUserId);
        if (hand.userPitchCardIds.some((id) => id == null)) {
            showError('핸드를 모두 채워주세요.');
            renderSlots();
            renderPool();
            return;
        }
        const userPitchCardIds = hand.userPitchCardIds.map(Number);
        const dropCardId = userPitchCardIds[userPitchCardIds.length - 1];

        try {
            await BluffBallTeamApi.saveMyPitchCards(
                state.team.teamId,
                formatSelect.value,
                userPitchCardIds,
                dropCardId,
            );
            showOk('내 구종 인스턴스 로드아웃을 저장했습니다.');
        } catch (e) {
            showError(e.message || String(e));
        }
    }

    formatSelect.addEventListener('change', () => {
        loadForFormat().catch((e) => showError(e.message || String(e)));
    });
    memberSelect.addEventListener('change', () => {
        state.selectedUserId = Number(memberSelect.value);
        renderSlots();
        renderPool();
        btnSaveCards.disabled = !state.team || !isEditingSelf();
    });
    btnSaveCards.addEventListener('click', () => save());

    loadPage().catch((e) => showError(e.message || String(e)));
})();
