package com.project.bluffball.domain.team.service.usecase.executor;

import com.project.bluffball.domain.team.entity.Team;
import com.project.bluffball.domain.team.entity.TeamTreasuryTransaction;
import com.project.bluffball.domain.team.repository.TeamRepository;
import com.project.bluffball.domain.team.repository.TeamTreasuryTransactionRepository;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import com.project.bluffball.domain.user.entity.User;
import com.project.bluffball.domain.user.repository.UserRepository;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀 재정(기부) 쓰기 전담 Executor (usecase/executor 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamTreasuryExecutor {

    private final TeamReader teamReader;
    private final TeamRepository teamRepository;
    private final TeamTreasuryTransactionRepository teamTreasuryTransactionRepository;
    private final UserRepository userRepository;

    /**
     * 유저 재화를 차감하고 팀 재정에 기부한다.
     *
     * @param userId 기부 유저 ID
     * @param teamId 팀 ID
     * @param amount 기부 금액
     * @return 팀 ID
     */
    @Transactional
    public Long donate(Long userId, Long teamId, long amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "userId=" + userId));
        Team team = teamReader.getById(teamId);

        user.spendCurrency(amount);
        team.receiveDonation(amount);
        teamTreasuryTransactionRepository.save(
                TeamTreasuryTransaction.donation(teamId, amount, userId));

        teamRepository.save(team);
        userRepository.save(user);
        return teamId;
    }
}
