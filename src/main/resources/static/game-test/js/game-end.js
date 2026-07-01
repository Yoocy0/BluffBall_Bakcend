(() => {
    const els = {
        inningSummary: document.getElementById('inningSummary'),
        homeScore: document.getElementById('homeScore'),
        awayScore: document.getElementById('awayScore'),
        winnerMsg: document.getElementById('winnerMsg'),
        lastTurnSummary: document.getElementById('lastTurnSummary'),
        matchIdLine: document.getElementById('matchIdLine'),
    };

    function readData() {
        const params = new URLSearchParams(window.location.search);
        const matchSessionId = params.get('matchSessionId') || '';

        const stored = BluffBallGameWs.getStoredGameEnd() || BluffBallGameWs.getStoredTurnResult();
        if (stored) {
            return { ...stored, matchSessionId: matchSessionId || stored.matchSessionId };
        }

        return null;
    }

    function addRow(label, value) {
        const div = document.createElement('div');
        div.innerHTML = `<dt>${label}:</dt><dd>${value}</dd>`;
        els.lastTurnSummary.appendChild(div);
    }

    function winnerText(homeScore, awayScore) {
        if (homeScore > awayScore) {
            return 'Home 팀 승리';
        }
        if (awayScore > homeScore) {
            return 'Away 팀 승리';
        }
        return '무승부';
    }

    const data = readData();

    if (!data || data.homeScore == null || data.awayScore == null) {
        if (els.inningSummary) {
            els.inningSummary.textContent = '경기 결과가 없습니다. 타자 선택부터 경기를 진행해 주세요.';
        }
        if (els.winnerMsg) {
            els.winnerMsg.textContent = '';
        }
        return;
    }

    const totalInnings = data.totalInnings ?? 1;
    if (els.inningSummary) {
        els.inningSummary.textContent = `${totalInnings}이닝 경기 종료`;
    }
    if (els.homeScore) {
        els.homeScore.textContent = String(data.homeScore);
    }
    if (els.awayScore) {
        els.awayScore.textContent = String(data.awayScore);
    }
    if (els.winnerMsg) {
        els.winnerMsg.textContent = winnerText(data.homeScore, data.awayScore);
    }

    if (data.turnResult) {
        addRow('판정', data.turnResult);
    }
    if (data.turnNumber != null) {
        addRow('턴', String(data.turnNumber));
    }
    if (data.inning != null) {
        addRow('이닝', `${data.inning}회 ${data.isTop ? '초' : '말'}`);
    }

    if (data.matchSessionId && els.matchIdLine) {
        els.matchIdLine.textContent = `matchSessionId: ${data.matchSessionId}`;
    }
})();
