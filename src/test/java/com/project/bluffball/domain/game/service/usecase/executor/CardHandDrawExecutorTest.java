package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.card.service.usecase.reader.UserPitchCardReader;
import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.enums.BotDifficulty;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.user.record.enums.GameMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardHandDrawExecutor")
class CardHandDrawExecutorTest {

    @Mock
    private MatchInfoRepository matchInfoRepository;
    @Mock
    private MatchInfoReader matchInfoReader;
    @Mock
    private UserPitchCardReader userPitchCardReader;
    @Mock
    private CardHandDrawer cardHandDrawer;
    @Mock
    private GameModeRule gameModeRule;

    @InjectMocks
    private CardHandDrawExecutor executor;

    @Test
    @DisplayName("사람 참가자는 보유 인스턴스 ID 풀에서 드로우한다")
    void humanUsesOwnedInstancePool() {
        String sessionId = "match-1";
        Long humanId = 10L;
        Long opponentId = 20L;
        MatchInfo matchInfo = MatchInfo.builder()
                .id(sessionId)
                .gameMode(GameMode.SHOWDOWN)
                .pitcherUserId(humanId)
                .batterLineup(List.of(opponentId))
                .build();
        matchInfo.ensureCollectionsInitialized();

        when(matchInfoReader.getById(sessionId)).thenReturn(matchInfo);
        when(gameModeRule.getHandSize(GameMode.SHOWDOWN)).thenReturn(3);
        when(userPitchCardReader.findOwnedInstanceIds(humanId))
                .thenReturn(List.of(101L, 102L, 103L, 104L));
        when(userPitchCardReader.findOwnedInstanceIds(opponentId))
                .thenReturn(List.of(201L, 202L, 203L));
        when(cardHandDrawer.draw(eq(List.of(101L, 102L, 103L, 104L)), eq(3)))
                .thenReturn(List.of(101L, 102L, 103L));
        when(cardHandDrawer.draw(eq(List.of(201L, 202L, 203L)), eq(3)))
                .thenReturn(List.of(201L, 202L, 203L));

        List<List<Long>> hands = executor.executeForAllParticipants(sessionId);

        assertThat(hands).containsExactly(
                List.of(101L, 102L, 103L),
                List.of(201L, 202L, 203L));
        verify(userPitchCardReader).findOwnedInstanceIds(humanId);
        verify(userPitchCardReader).findOwnedInstanceIds(opponentId);
        verify(matchInfoRepository).save(matchInfo);
    }

    @Test
    @DisplayName("연습 봇은 botDrawPool을 쓰고 사람만 보유 인스턴스 풀을 쓴다")
    void botKeepsBotDrawPool() {
        String sessionId = "bot-match";
        Long humanId = 10L;
        Long botId = 99L;
        MatchInfo matchInfo = MatchInfo.builder()
                .id(sessionId)
                .gameMode(GameMode.BOT)
                .pitcherUserId(humanId)
                .batterLineup(List.of(botId))
                .build();
        matchInfo.ensureCollectionsInitialized();
        matchInfo.markAsPracticeBotMatch(
                BotDifficulty.NORMAL, botId, List.of(901L, 902L, 903L, 904L));

        when(matchInfoReader.getById(sessionId)).thenReturn(matchInfo);
        when(gameModeRule.getHandSize(GameMode.BOT)).thenReturn(3);
        when(userPitchCardReader.findOwnedInstanceIds(humanId))
                .thenReturn(List.of(101L, 102L, 103L));
        when(cardHandDrawer.draw(eq(List.of(101L, 102L, 103L)), eq(3)))
                .thenReturn(List.of(101L, 102L, 103L));
        when(cardHandDrawer.draw(eq(List.of(901L, 902L, 903L, 904L)), eq(3)))
                .thenReturn(List.of(901L, 902L, 903L));

        executor.executeForAllParticipants(sessionId);

        verify(userPitchCardReader).findOwnedInstanceIds(humanId);
        verify(userPitchCardReader, never()).findOwnedInstanceIds(botId);
        verify(cardHandDrawer).draw(eq(List.of(101L, 102L, 103L)), eq(3));
        verify(cardHandDrawer).draw(eq(List.of(901L, 902L, 903L, 904L)), eq(3));
        verify(matchInfoRepository).save(matchInfo);
    }
}
