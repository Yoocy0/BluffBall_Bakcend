(() => {
    const ROLE_KEY = 'bluffball.role';
    const PITCHER_USER_ID_KEY = 'bluffball.pitcherUserId';

    function setRole(role) {
        sessionStorage.setItem(ROLE_KEY, role);
    }

    function getRole() {
        return sessionStorage.getItem(ROLE_KEY);
    }

    function isPitcher() {
        return getRole() === 'pitcher';
    }

    function isBatter() {
        return getRole() === 'batter';
    }

    function getMyUserId() {
        const raw = window.BluffBallAuth?.getUserIdFromToken?.();
        if (raw == null || raw === '') {
            return null;
        }
        const parsed = Number(raw);
        return Number.isNaN(parsed) ? null : parsed;
    }

    function syncRoleFromPitcherUserId(pitcherUserId) {
        if (pitcherUserId == null) {
            return;
        }
        sessionStorage.setItem(PITCHER_USER_ID_KEY, String(pitcherUserId));
        const myId = getMyUserId();
        if (myId == null) {
            return;
        }
        if (myId === Number(pitcherUserId)) {
            setRole('pitcher');
        } else {
            setRole('batter');
        }
    }

    function ensureRoleSyncedFromStorage() {
        const raw = sessionStorage.getItem(PITCHER_USER_ID_KEY);
        if (raw != null && raw !== '') {
            syncRoleFromPitcherUserId(Number(raw));
        }
    }

    function requireLoginOrRedirect() {
        if (!window.BluffBallAuth?.isLoggedIn?.()) {
            window.location.href = '/game-test/Home.html';
            return false;
        }
        return true;
    }

    window.BluffBallRole = {
        setRole,
        getRole,
        isPitcher,
        isBatter,
        getMyUserId,
        syncRoleFromPitcherUserId,
        ensureRoleSyncedFromStorage,
        requireLoginOrRedirect,
    };
})();
