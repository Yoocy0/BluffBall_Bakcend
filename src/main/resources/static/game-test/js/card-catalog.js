(() => {
    const STORAGE_COORDINATES = 'bluffball.coordinateCards';
    const STORAGE_PITCH_CARDS = 'bluffball.pitchCards';

    const TIMING_LABELS = {
        TOO_EARLY: '너무 이른',
        EARLY: '이른',
        NORMAL: '보통',
        LATE: '늦은',
        TOO_LATE: '너무 늦은',
    };

    const DIRECTION_LABELS = {
        DOWN: '아래',
        SIDE: '옆(→)',
        REVERSE: '역(←)',
    };

    const PITCH_FLAVOR = {
        '포심 패스트볼': '가장 기본적인 직구. 시작 좌표에서 궤적 변화가 없어 예측이 비교적 쉽지만, 빠른 타이밍에 맞춰야 타격이 유리합니다.',
        '커터': '빠른 횡변화 구종. 시작 좌표에서 옆으로 1칸 휘어집니다. 빠른 타이밍(EARLY)에 맞춰야 합니다.',
        '스플리터': '빠른 낙차 구종. 시작 좌표에서 아래로 1칸 떨어집니다. 빠른 타이밍(EARLY)에 맞춰야 합니다.',
        '슬라이더': '횡변화 구종. 시작 좌표에서 옆으로 크게 휘어져 최종 좌표가 달라집니다.',
        '커브': '낙차가 큰 변화구. 아래로 크게 떨어지며 늦은 타이밍에 맞춰야 합니다.',
        '포크': '아래로 떨어지는 변화구. 슬라이더보다 낙차는 작지만 중간 타이밍에 맞춰야 합니다.',
    };

    function formatTiming(timing) {
        return TIMING_LABELS[timing] || timing || '-';
    }

    function formatChange(changeAmount, direction) {
        if (changeAmount === 0) {
            return '좌표 변화 없음';
        }
        const dir = DIRECTION_LABELS[direction] || direction;
        return `${dir} ${changeAmount}칸`;
    }

    function buildPitchTipLines(card) {
        const lines = [
            formatChange(card.changeAmount, card.direction),
            `구종 타이밍: ${formatTiming(card.timing)}`,
            '최종 좌표 = 시작 좌표 + 구종 변화 (격자 이탈 시 0·폭투)',
        ];
        const flavor = PITCH_FLAVOR[card.name];
        if (flavor) {
            lines.push(flavor);
        }
        return lines;
    }

    async function fetchPitchCards() {
        const cached = sessionStorage.getItem(STORAGE_PITCH_CARDS);
        if (cached) {
            return JSON.parse(cached);
        }

        const token = BluffBallWs.requireLoginToken();
        const res = await fetch('/api/v1/cards/pitch', {
            headers: { Authorization: `Bearer ${token}` },
        });
        if (!res.ok) {
            throw new Error(`구종 카드 조회 실패 (HTTP ${res.status})`);
        }

        const cards = await res.json();
        sessionStorage.setItem(STORAGE_PITCH_CARDS, JSON.stringify(cards));
        return cards;
    }

    /** 내 보유 구종 인스턴스 (userPitchCardId) */
    async function fetchMyPitchCards() {
        const token = BluffBallWs.requireLoginToken();
        const res = await fetch('/api/v1/users/me/pitch-cards', {
            headers: { Authorization: `Bearer ${token}` },
        });
        if (!res.ok) {
            throw new Error(`내 구종 인스턴스 조회 실패 (HTTP ${res.status})`);
        }
        return res.json();
    }

    /** 투수 시작 좌표 선택용 — coordinateNumber 1~25 */
    async function fetchPitcherCoordinateOptions() {
        const cached = sessionStorage.getItem(STORAGE_COORDINATES);
        if (cached) {
            return JSON.parse(cached);
        }

        const token = BluffBallWs.requireLoginToken();
        const res = await fetch('/api/v1/cards/coordinate', {
            headers: { Authorization: `Bearer ${token}` },
        });
        if (!res.ok) {
            throw new Error(`좌표 카드 조회 실패 (HTTP ${res.status})`);
        }

        const all = await res.json();
        const pitcherCoords = all
            .filter((c) => c.coordinateNumber >= 1 && c.coordinateNumber <= 25)
            .sort((a, b) => a.coordinateNumber - b.coordinateNumber);

        sessionStorage.setItem(STORAGE_COORDINATES, JSON.stringify(pitcherCoords));
        return pitcherCoords;
    }

    /**
     * 타자 대기 화면 우측 구종 tip 레일을 렌더한다.
     * @param {HTMLElement} listEl
     * @param {Array} cards
     */
    function mountPitchTipsRail(listEl, cards) {
        if (!listEl) {
            return;
        }
        listEl.innerHTML = '';

        let openTip = null;
        let openBtn = null;

        cards.forEach((card) => {
            const item = document.createElement('div');
            item.className = 'pitch-tip-item';

            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'pitch-tip-btn';
            btn.textContent = card.name;
            btn.setAttribute('aria-expanded', 'false');

            const tip = document.createElement('div');
            tip.className = 'pitch-tip-popover';
            tip.hidden = true;
            tip.setAttribute('role', 'tooltip');

            const title = document.createElement('p');
            title.className = 'pitch-tip-popover-title';
            title.textContent = card.name;

            const body = document.createElement('ul');
            body.className = 'pitch-tip-popover-body';
            buildPitchTipLines(card).forEach((line) => {
                const li = document.createElement('li');
                li.textContent = line;
                body.appendChild(li);
            });

            tip.appendChild(title);
            tip.appendChild(body);

            btn.addEventListener('click', () => {
                const willOpen = tip.hidden;
                if (openTip && openTip !== tip) {
                    openTip.hidden = true;
                    openBtn?.setAttribute('aria-expanded', 'false');
                    openBtn?.classList.remove('is-open');
                }
                tip.hidden = !willOpen;
                btn.setAttribute('aria-expanded', willOpen ? 'true' : 'false');
                btn.classList.toggle('is-open', willOpen);
                if (willOpen) {
                    openTip = tip;
                    openBtn = btn;
                } else {
                    openTip = null;
                    openBtn = null;
                }
            });

            item.appendChild(btn);
            item.appendChild(tip);
            listEl.appendChild(item);
        });
    }

    window.BluffBallCards = {
        fetchPitcherCoordinateOptions,
        fetchPitchCards,
        fetchMyPitchCards,
        mountPitchTipsRail,
        formatTiming,
        formatChange,
    };
})();
