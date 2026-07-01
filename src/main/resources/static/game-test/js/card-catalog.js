(() => {
    const STORAGE_COORDINATES = 'bluffball.coordinateCards';

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

    window.BluffBallCards = {
        fetchPitcherCoordinateOptions,
    };
})();
