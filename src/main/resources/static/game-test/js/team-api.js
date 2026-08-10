(() => {
    function authHeaders(json = false) {
        const token = BluffBallWs.requireLoginToken();
        const headers = { Authorization: `Bearer ${token}` };
        if (json) {
            headers['Content-Type'] = 'application/json';
        }
        return headers;
    }

    async function parseError(res) {
        const body = await res.json().catch(() => ({}));
        return body.message || `요청 실패 (HTTP ${res.status})`;
    }

    async function getMyTeam() {
        const res = await fetch('/api/v1/teams/me', { headers: authHeaders() });
        if (res.status === 404) {
            return null;
        }
        if (!res.ok) {
            throw new Error(await parseError(res));
        }
        return res.json();
    }

    async function createTeam(name) {
        const res = await fetch('/api/v1/teams', {
            method: 'POST',
            headers: authHeaders(true),
            body: JSON.stringify({ name }),
        });
        if (!res.ok) {
            throw new Error(await parseError(res));
        }
        return res.json();
    }

    async function getMembers(teamId) {
        const res = await fetch(`/api/v1/teams/${teamId}/members`, { headers: authHeaders() });
        if (!res.ok) {
            throw new Error(await parseError(res));
        }
        return res.json();
    }

    async function getLineup(teamId, format) {
        const res = await fetch(`/api/v1/teams/${teamId}/lineups/${format}`, {
            headers: authHeaders(),
        });
        if (res.status === 404) {
            return null;
        }
        if (!res.ok) {
            throw new Error(await parseError(res));
        }
        return res.json();
    }

    async function saveLineup(teamId, format, userIds, startingPitcherUserId) {
        const res = await fetch(`/api/v1/teams/${teamId}/lineups/${format}`, {
            method: 'PUT',
            headers: authHeaders(true),
            body: JSON.stringify({ userIds, startingPitcherUserId }),
        });
        if (!res.ok) {
            throw new Error(await parseError(res));
        }
        return res.json();
    }

    async function getPitchCards(teamId, format) {
        const res = await fetch(`/api/v1/teams/${teamId}/lineups/${format}/pitch-cards`, {
            headers: authHeaders(),
        });
        if (res.status === 404) {
            return null;
        }
        if (!res.ok) {
            throw new Error(await parseError(res));
        }
        return res.json();
    }

    async function savePitchCards(teamId, format, selections) {
        const res = await fetch(`/api/v1/teams/${teamId}/lineups/${format}/pitch-cards`, {
            method: 'PUT',
            headers: authHeaders(true),
            body: JSON.stringify({ selections }),
        });
        if (!res.ok) {
            throw new Error(await parseError(res));
        }
        return res.json();
    }

    /** 본인 로드아웃 저장 (userPitchCardIds) */
    async function saveMyPitchCards(teamId, format, userPitchCardIds, dropCardId) {
        const res = await fetch(`/api/v1/teams/${teamId}/lineups/${format}/pitch-cards/me`, {
            method: 'PUT',
            headers: authHeaders(true),
            body: JSON.stringify({ userPitchCardIds, dropCardId }),
        });
        if (!res.ok) {
            throw new Error(await parseError(res));
        }
        return res.json();
    }

    /**
     * 테스트용: 선택한 포맷 기준 빈자리를 봇으로 채운다.
     * 부족한 인원 수는 서버가 format(Compact 4 / Full 9)으로 계산한다.
     */
    async function fillRosterWithBots(leaderUserId, teamId, format, myRole) {
        const res = await fetch('/game-test/api/bots/league/fill-roster', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                leaderUserId: Number(leaderUserId),
                teamId: teamId != null ? Number(teamId) : null,
                format,
                myRole: myRole || 'BATTER',
            }),
        });
        if (!res.ok) {
            throw new Error(await parseError(res));
        }
        return res.json();
    }

    window.BluffBallTeamApi = {
        getMyTeam,
        createTeam,
        getMembers,
        getLineup,
        saveLineup,
        getPitchCards,
        savePitchCards,
        saveMyPitchCards,
        fillRosterWithBots,
    };
})();
