(() => {
    const btnStart = document.getElementById('btnStart');
    const startError = document.getElementById('startError');

    btnStart?.addEventListener('click', async () => {
        btnStart.disabled = true;
        if (startError) {
            startError.hidden = true;
        }

        try {
            const res = await fetch('/game-test/api/match', { method: 'POST' });
            if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
            }
            const data = await res.json();
            sessionStorage.clear();
            sessionStorage.setItem('bluffball.matchSessionId', data.matchSessionId);
            window.location.href =
                `/game-test/SetupNumber.html?matchSessionId=${encodeURIComponent(data.matchSessionId)}`;
        } catch (e) {
            if (startError) {
                startError.textContent = `게임 시작 실패: ${e.message}`;
                startError.hidden = false;
            }
            btnStart.disabled = false;
        }
    });
})();
