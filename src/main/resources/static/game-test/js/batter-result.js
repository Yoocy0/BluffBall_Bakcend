(() => {
    const els = {
        turnResult: document.getElementById('turnResult'),
        resultDetail: document.getElementById('resultDetail'),
        scoreboard: document.getElementById('scoreboard'),
        linkBack: document.getElementById('linkBack'),
        btnNextPitch: document.getElementById('btnNextPitch'),
        gameOverMsg: document.getElementById('gameOverMsg'),
    };

    function readData() {
        const params = new URLSearchParams(window.location.search);
        const matchSessionId = params.get('matchSessionId') || '';

        try {
            const stored = JSON.parse(sessionStorage.getItem('bluffball.batterResult') || 'null');
            if (stored) {
                return { ...stored, matchSessionId: matchSessionId || stored.matchSessionId };
            }
        } catch (_) {
            /* ignore */
        }
        return null;
    }

    function addRow(label, value) {
        const div = document.createElement('div');
        div.innerHTML = `<dt>${label}:</dt><dd>${value}</dd>`;
        els.scoreboard.appendChild(div);
    }

    const data = readData();

    if (data?.matchSessionId && els.linkBack) {
        els.linkBack.href = `/game-test/BatterSelect.html?matchSessionId=${encodeURIComponent(data.matchSessionId)}`;
    }

    if (!data || !data.turnResult) {
        els.turnResult.textContent = '?';
        els.resultDetail.textContent = '결과 없음 — 타자 선택 화면에서 제출하세요.';
        return;
    }

    const dice = (data.diceResults && data.diceResults.length > 0)
        ? data.diceResults.join(' + ')
        : '-';

    els.turnResult.textContent = data.turnResult;
    els.resultDetail.textContent =
        `전송 ${data.responseTimeSec}s · 최종 좌표 ${data.finalCoordinateNumber} · `
        + `투구 타이밍 ${data.pitchTiming || '-'} · 주사위 [${dice}]`;

    addRow('턴', String(data.turnNumber ?? '-'));
    addRow('이닝', `${data.inning}회 ${data.isTop ? '초' : '말'} / ${data.totalInnings ?? 1}이닝`);
    addRow('점수', `어웨이 ${data.awayScore} : ${data.homeScore} 홈`);
    addRow('카운트', `B${data.balls} S${data.strikes} O${data.outs}`);
    addRow('주자', [
        data.firstBase ? '1루' : '',
        data.secondBase ? '2루' : '',
        data.thirdBase ? '3루' : '',
    ].filter(Boolean).join(' ') || '없음');

    if (data.gameOver) {
        sessionStorage.setItem('bluffball.gameEnd', JSON.stringify(data));
        const qs = new URLSearchParams({ matchSessionId: data.matchSessionId });
        window.location.href = `/game-test/GameEnd.html?${qs.toString()}`;
        return;
    }

    if (els.btnNextPitch && data.matchSessionId) {
        els.btnNextPitch.addEventListener('click', () => {
            sessionStorage.setItem('bluffball.matchSessionId', data.matchSessionId);
            window.location.href =
                `/game-test/PitcherSelect.html?matchSessionId=${encodeURIComponent(data.matchSessionId)}&loop=1`;
        });
    }
})();
