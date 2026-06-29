(() => {
    const els = {
        startCoordinate: document.getElementById('startCoordinate'),
        finalCoordinate: document.getElementById('finalCoordinate'),
        resultDetail: document.getElementById('resultDetail'),
        coordGrid: document.getElementById('coordGrid'),
        finalOutside: document.getElementById('finalOutside'),
        linkBack: document.getElementById('linkBack'),
    };

    function readParams() {
        const params = new URLSearchParams(window.location.search);
        let start = params.get('start');
        let finalCoord = params.get('final');
        let pitch = params.get('pitch') || '';
        let matchSessionId = params.get('matchSessionId') || '';

        if (start == null || finalCoord == null) {
            try {
                const stored = JSON.parse(sessionStorage.getItem('bluffball.pitcherResult') || '{}');
                if (start == null && stored.startCoordinateNumber != null) {
                    start = String(stored.startCoordinateNumber);
                }
                if (finalCoord == null && stored.finalCoordinateNumber != null) {
                    finalCoord = String(stored.finalCoordinateNumber);
                }
                pitch = pitch || stored.pitchCardName || '';
                matchSessionId = matchSessionId || stored.matchSessionId || '';
            } catch (_) {
                /* ignore */
            }
        }

        return {
            start: start != null ? Number(start) : null,
            finalCoord: finalCoord != null ? Number(finalCoord) : null,
            pitch,
            matchSessionId,
        };
    }

    function renderGrid(startNumber, finalNumber) {
        els.coordGrid.innerHTML = '';
        for (let n = 1; n <= 25; n++) {
            const cell = document.createElement('div');
            cell.className = 'coord-btn';
            const x = (n - 1) % 5;
            const y = Math.floor((n - 1) / 5);
            if (x >= 1 && x <= 3 && y >= 1 && y <= 3) {
                cell.classList.add('strike-zone');
            }
            if (n === startNumber) {
                cell.classList.add('start-selected');
            }
            if (n === finalNumber && finalNumber >= 1) {
                cell.classList.add('final-selected');
            }
            cell.textContent = n;
            els.coordGrid.appendChild(cell);
        }
    }

    function formatFinal(n) {
        if (n === 0) return '0 (격자 밖)';
        return String(n);
    }

    const { start, finalCoord, pitch, matchSessionId } = readParams();

    if (matchSessionId && els.linkBack) {
        els.linkBack.href = `/game-test/PitcherSelect.html?matchSessionId=${encodeURIComponent(matchSessionId)}`;
    }

    if (start == null || Number.isNaN(start)) {
        els.startCoordinate.textContent = '?';
        els.finalCoordinate.textContent = '?';
        els.resultDetail.textContent = '결과 데이터가 없습니다. 투수 선택 화면에서 투구를 제출하세요.';
        renderGrid(null, null);
        return;
    }

    els.startCoordinate.textContent = String(start);
    els.finalCoordinate.textContent = finalCoord != null && !Number.isNaN(finalCoord)
        ? formatFinal(finalCoord)
        : '?';

    if (finalCoord === 0) {
        els.finalOutside.hidden = false;
    }

    const pitchLabel = pitch ? `구종: ${pitch} · ` : '';
    els.resultDetail.textContent = finalCoord != null && !Number.isNaN(finalCoord)
        ? `${pitchLabel}시작 ${start} → 최종 ${formatFinal(finalCoord)} (테스트 화면에서만 최종 좌표 표시)`
        : `${pitchLabel}시작 좌표 = ${start}`;

    renderGrid(start, finalCoord);
})();
