(() => {
    const COUNTDOWN_SEC = 3;
    const DICE_ROLL_MS = 1800;
    const DICE_TICK_MS = 70;

    const { goWithMatch, mountBroadcastHud, renderBroadcastHud } = window.BluffBallNav;

    const diceRollOverlay = document.getElementById('diceRollOverlay');
    const diceStage = document.getElementById('diceStage');
    const resultCard = document.getElementById('resultCard');
    const turnResultEl = document.getElementById('turnResult');
    const resultDetailEl = document.getElementById('resultDetail');
    const countdownEl = document.getElementById('countdown');

    function readData() {
        try {
            return JSON.parse(sessionStorage.getItem('bluffball.batterResult') || 'null');
        } catch (_) {
            return null;
        }
    }

    function formatTurnResult(name) {
        const map = {
            STRIKE: '스트라이크',
            BALL: '볼',
            SINGLE: '1루타',
            DOUBLE: '2루타',
            TRIPLE: '3루타',
            HOMERUN: '홈런',
            OUT: '아웃',
            DOUBLE_PLAY: '병살',
            WILD_PITCH: '폭투',
            WALK: '볼넷',
            STRIKE_OUT: '삼진',
        };
        return map[name] || name;
    }

    function hasHitEvent(data) {
        return Array.isArray(data.diceResults) && data.diceResults.length > 0;
    }

    function renderResultContent(data) {
        const dice = hasHitEvent(data)
            ? data.diceResults.join(' + ')
            : null;

        turnResultEl.textContent = formatTurnResult(data.turnResult);
        resultDetailEl.textContent = [
            dice ? `주사위 ${dice}` : null,
            `B${data.balls} S${data.strikes} O${data.outs}`,
            `AWAY ${data.awayScore} : ${data.homeScore} HOME`,
        ].filter(Boolean).join(' · ');
    }

    function startCountdown(onDone) {
        let left = COUNTDOWN_SEC;
        countdownEl.textContent = `${left}초 후 다음 투구…`;
        const id = setInterval(() => {
            left -= 1;
            if (left <= 0) {
                clearInterval(id);
                onDone();
                return;
            }
            countdownEl.textContent = `${left}초 후 다음 투구…`;
        }, 1000);
    }

    function navigateNext(data) {
        if (data.gameOver) {
            sessionStorage.setItem('bluffball.gameEnd', JSON.stringify(data));
            goWithMatch('/game-test/GameEnd.html');
            return;
        }
        goWithMatch('/game-test/PitcherSelect.html');
    }

    function showResultCard(data) {
        renderResultContent(data);
        resultCard.hidden = false;
        resultCard.classList.add('is-visible');
        startCountdown(() => navigateNext(data));
    }

    function buildDie(finalValue) {
        const die = document.createElement('div');
        die.className = 'die die-rolling';
        const face = document.createElement('span');
        face.className = 'die-face';
        face.textContent = '1';
        die.appendChild(face);
        return { el: die, face, finalValue };
    }

    function rollDiceAnimation(finalValues) {
        return new Promise((resolve) => {
            diceStage.innerHTML = '';
            const dice = finalValues.map((value) => buildDie(value));
            dice.forEach((d) => diceStage.appendChild(d.el));

            diceRollOverlay.hidden = false;
            diceRollOverlay.classList.add('is-active');

            const startedAt = Date.now();
            const tickId = setInterval(() => {
                dice.forEach((d) => {
                    if (d.el.classList.contains('die-rolling')) {
                        d.face.textContent = String(Math.floor(Math.random() * 6) + 1);
                    }
                });

                if (Date.now() - startedAt >= DICE_ROLL_MS) {
                    clearInterval(tickId);
                    dice.forEach((d, index) => {
                        d.face.textContent = String(d.finalValue);
                        d.el.classList.remove('die-rolling');
                        d.el.classList.add('die-settled');
                        d.el.style.animationDelay = `${index * 0.08}s`;
                    });
                    setTimeout(() => {
                        diceRollOverlay.classList.remove('is-active');
                        diceRollOverlay.hidden = true;
                        resolve();
                    }, 550);
                }
            }, DICE_TICK_MS);
        });
    }

    async function run() {
        const data = readData();
        if (!data?.turnResult) {
            goWithMatch('/game-test/Home.html');
            return;
        }

        sessionStorage.removeItem('bluffball.batterCoordinate');
        sessionStorage.removeItem('bluffball.batterTimerStartedAt');
        sessionStorage.removeItem('bluffball.selectedPitchCard');

        mountBroadcastHud().then((status) => {
            if (status) {
                renderBroadcastHud(document.getElementById('broadcastHud'), {
                    ...status,
                    balls: data.balls,
                    strikes: data.strikes,
                    outs: data.outs,
                    homeScore: data.homeScore,
                    awayScore: data.awayScore,
                    firstBase: data.firstBase,
                    secondBase: data.secondBase,
                    thirdBase: data.thirdBase,
                    inning: data.inning,
                    isTop: data.isTop,
                });
            }
        });

        if (hasHitEvent(data)) {
            await rollDiceAnimation(data.diceResults);
        }

        showResultCard(data);
    }

    run();
})();
