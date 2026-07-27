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
        catalog: [],
        rosterUserIds: [],
        memberLabels: new Map(),
        /** userId -> { cardIds: (number|null)[], dropIndex: number } */
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

    function ensureUserHand(userId) {
        if (!state.byUser.has(userId)) {
            const size = handSize(formatSelect.value);
            state.byUser.set(userId, {
                cardIds: Array.from({ length: size }, () => null),
                dropIndex: size - 1,
            });
        }
        return state.byUser.get(userId);
    }

    function cardName(cardId) {
        const c = state.catalog.find((x) => x.cardId === cardId || x.id === cardId);
        return c ? c.name : `card ${cardId}`;
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

    function placeCard(cardId, slotIndex) {
        const hand = ensureUserHand(state.selectedUserId);
        // remove duplicate elsewhere in same hand
        hand.cardIds = hand.cardIds.map((id, i) => (id === cardId && i !== slotIndex ? null : id));
        hand.cardIds[slotIndex] = cardId;
        renderSlots();
        renderPool();
    }

    function clearSlot(slotIndex) {
        const hand = ensureUserHand(state.selectedUserId);
        hand.cardIds[slotIndex] = null;
        renderSlots();
        renderPool();
    }

    function renderSlots() {
        cardSlots.innerHTML = '';
        if (state.selectedUserId == null) {
            cardSlots.textContent = '라인업 멤버가 없습니다. 팀 페이지에서 먼저 저장하세요.';
            return;
        }

        const hand = ensureUserHand(state.selectedUserId);
        const size = hand.cardIds.length;
        hand.cardIds.forEach((cardId, index) => {
            const isPlus = index === size - 1;
            const slot = document.createElement('div');
            slot.className = isPlus ? 'slot-card slot-card-plus' : 'slot-card';

            const label = document.createElement('div');
            label.className = 'slot-label';
            label.textContent = isPlus ? '+1 (교체 제외)' : `${index + 1}번`;
            slot.appendChild(label);

            const body = document.createElement('div');
            body.className = 'slot-body';
            body.textContent = cardId != null ? cardName(cardId) : '비움';
            slot.appendChild(body);

            if (cardId != null) {
                const clearBtn = document.createElement('button');
                clearBtn.type = 'button';
                clearBtn.className = 'slot-clear';
                clearBtn.textContent = '×';
                clearBtn.addEventListener('click', () => clearSlot(index));
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
                if (data.cardId != null) {
                    placeCard(Number(data.cardId), index);
                }
            });

            cardSlots.appendChild(slot);
        });
    }

    function renderPool() {
        cardPool.innerHTML = '';
        const hand = state.selectedUserId != null ? ensureUserHand(state.selectedUserId) : null;
        const used = new Set((hand?.cardIds || []).filter((id) => id != null));

        state.catalog.forEach((card) => {
            const cardId = card.cardId;
            const chip = document.createElement('div');
            chip.className = 'drag-chip drag-chip-card';
            chip.draggable = true;
            if (used.has(cardId)) {
                chip.classList.add('drag-chip-used');
            }
            chip.innerHTML = `<strong>${card.name}</strong>`
                + `<span>${card.direction || '-'} ${card.changeAmount ?? 0} · ${card.timing || '-'}</span>`;
            chip.addEventListener('dragstart', (e) => onDragStart(e, { cardId }));
            cardPool.appendChild(chip);
        });
    }

    function renderMemberSelect() {
        memberSelect.innerHTML = '';
        state.rosterUserIds.forEach((userId) => {
            const opt = document.createElement('option');
            opt.value = String(userId);
            opt.textContent = state.memberLabels.get(userId) || `user ${userId}`;
            memberSelect.appendChild(opt);
        });
        if (state.rosterUserIds.length) {
            if (!state.rosterUserIds.includes(state.selectedUserId)) {
                state.selectedUserId = state.rosterUserIds[0];
            }
            memberSelect.value = String(state.selectedUserId);
        } else {
            state.selectedUserId = null;
        }
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
                cardIds: Array.from({ length: size }, () => null),
                dropIndex: size - 1,
            });
        });

        const saved = await BluffBallTeamApi.getPitchCards(state.team.teamId, format);
        if (saved?.selections) {
            saved.selections.forEach((sel) => {
                const ids = [...(sel.cardIds || [])];
                while (ids.length < size) {
                    ids.push(null);
                }
                const dropIndex = Math.max(0, ids.indexOf(sel.dropCardId));
                // keep drop card at last slot for UI
                if (sel.dropCardId != null) {
                    const withoutDrop = ids.filter((id) => id !== sel.dropCardId);
                    const arranged = [...withoutDrop.slice(0, size - 1)];
                    while (arranged.length < size - 1) {
                        arranged.push(null);
                    }
                    arranged.push(sel.dropCardId);
                    state.byUser.set(sel.userId, { cardIds: arranged, dropIndex: size - 1 });
                } else {
                    state.byUser.set(sel.userId, {
                        cardIds: ids.slice(0, size),
                        dropIndex: dropIndex >= 0 ? dropIndex : size - 1,
                    });
                }
            });
        }

        state.selectedUserId = roster[0] ?? null;
        render();
    }

    async function loadPage() {
        if (!BluffBallAuth.isLoggedIn()) {
            window.location.href = '/game-test/Home.html';
            return;
        }

        state.team = await BluffBallTeamApi.getMyTeam();
        if (!state.team) {
            teamMeta.textContent = '소속 팀이 없습니다. 팀 페이지에서 창단하세요.';
            btnSaveCards.disabled = true;
            render();
            return;
        }

        teamMeta.textContent = `${state.team.name} · teamId ${state.team.teamId}`;
        btnSaveCards.disabled = false;

        const members = await BluffBallTeamApi.getMembers(state.team.teamId);
        state.memberLabels = new Map(
            members.map((m) => [m.userId, `${m.nickname || '멤버'} (#${m.userId})`]),
        );

        state.catalog = await BluffBallCards.fetchPitchCards();
        await loadForFormat();
    }

    async function save() {
        showError('');
        showOk('');
        if (!state.team) {
            showError('팀이 없습니다.');
            return;
        }
        if (!state.rosterUserIds.length) {
            showError('라인업이 없습니다.');
            return;
        }

        const selections = [];
        for (const userId of state.rosterUserIds) {
            const hand = ensureUserHand(userId);
            if (hand.cardIds.some((id) => id == null)) {
                showError(`${state.memberLabels.get(userId) || userId} 핸드를 모두 채워주세요.`);
                state.selectedUserId = userId;
                memberSelect.value = String(userId);
                renderSlots();
                renderPool();
                return;
            }
            const dropCardId = hand.cardIds[hand.cardIds.length - 1];
            selections.push({
                userId,
                cardIds: hand.cardIds.map(Number),
                dropCardId: Number(dropCardId),
            });
        }

        try {
            await BluffBallTeamApi.savePitchCards(state.team.teamId, formatSelect.value, selections);
            showOk('구종 카드를 저장했습니다.');
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
    });
    btnSaveCards.addEventListener('click', () => save());

    loadPage().catch((e) => showError(e.message || String(e)));
})();
