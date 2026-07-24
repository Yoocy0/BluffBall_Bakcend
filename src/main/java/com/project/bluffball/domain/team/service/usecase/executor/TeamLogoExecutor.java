package com.project.bluffball.domain.team.service.usecase.executor;

import com.project.bluffball.domain.team.entity.Team;
import com.project.bluffball.domain.team.repository.TeamRepository;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀 로고 변경 쓰기 전담 Executor (usecase/executor 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamLogoExecutor {

    private final TeamReader teamReader;
    private final TeamRepository teamRepository;

    /**
     * 팀 로고 URL을 변경한다.
     *
     * @param teamId 팀 ID
     * @param logoUrl 새 로고 URL
     * @return 팀 ID
     */
    @Transactional
    public Long updateLogo(Long teamId, String logoUrl) {
        Team team = teamReader.getById(teamId);
        team.updateLogo(logoUrl);
        teamRepository.save(team);
        return teamId;
    }
}
