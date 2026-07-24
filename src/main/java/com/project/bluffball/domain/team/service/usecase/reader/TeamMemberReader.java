package com.project.bluffball.domain.team.service.usecase.reader;

import com.project.bluffball.domain.team.dto.response.TeamMemberResponse;
import com.project.bluffball.domain.team.entity.TeamMember;
import com.project.bluffball.domain.team.enums.TeamMemberRole;
import com.project.bluffball.domain.team.repository.TeamMemberRepository;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 팀 멤버 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamMemberReader {

    private final TeamMemberRepository teamMemberRepository;
    private final UserReader userReader;
    private final TeamPresenceReader teamPresenceReader;

    /**
     * 멤버 Entity를 조회한다. Executor·Reader 내부 전용 (Service에서 호출 금지).
     *
     * @param teamId 팀 ID
     * @param userId 유저 ID
     * @return 멤버 Entity
     * @throws NotFoundException 멤버가 없으면
     */
    public TeamMember getByTeamIdAndUserId(Long teamId, Long userId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.TEAM_MEMBER_NOT_FOUND,
                        "teamId=" + teamId + ", userId=" + userId));
    }

    /**
     * 유저가 어떤 팀에든 소속되어 있는지 반환한다.
     *
     * @param userId 유저 ID
     * @return 소속이면 true
     */
    public boolean existsByUserId(Long userId) {
        return teamMemberRepository.existsByUserId(userId);
    }

    /**
     * 유저의 소속 팀 ID를 반환한다.
     *
     * @param userId 유저 ID
     * @return 팀 ID Optional
     */
    public Optional<Long> findTeamIdByUserId(Long userId) {
        return teamMemberRepository.findByUserId(userId).map(TeamMember::getTeamId);
    }

    /**
     * 해당 팀 멤버인지 반환한다.
     *
     * @param teamId 팀 ID
     * @param userId 유저 ID
     * @return 멤버이면 true
     */
    public boolean isMember(Long teamId, Long userId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId).isPresent();
    }

    /**
     * 해당 팀 리더인지 반환한다.
     *
     * @param teamId 팀 ID
     * @param userId 유저 ID
     * @return 리더이면 true
     */
    public boolean isLeader(Long teamId, Long userId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .map(member -> member.getRole() == TeamMemberRole.LEADER)
                .orElse(false);
    }

    /**
     * 팀 멤버 목록 DTO를 반환한다. (온/오프라인 포함)
     *
     * @param teamId 팀 ID
     * @return 멤버 응답 목록
     */
    public List<TeamMemberResponse> getMemberResponses(Long teamId) {
        return teamMemberRepository.findByTeamId(teamId).stream()
                .map(member -> new TeamMemberResponse(
                        member.getUserId(),
                        userReader.getNickname(member.getUserId()),
                        member.getRole(),
                        teamPresenceReader.isOnline(teamId, member.getUserId()),
                        member.getJoinedAt()))
                .toList();
    }

    /**
     * 단일 멤버 DTO를 반환한다.
     *
     * @param teamId 팀 ID
     * @param userId 유저 ID
     * @return 멤버 응답 DTO
     */
    public TeamMemberResponse getMemberResponse(Long teamId, Long userId) {
        TeamMember member = getByTeamIdAndUserId(teamId, userId);
        return new TeamMemberResponse(
                member.getUserId(),
                userReader.getNickname(member.getUserId()),
                member.getRole(),
                teamPresenceReader.isOnline(teamId, member.getUserId()),
                member.getJoinedAt());
    }

    /**
     * 팀 멤버 수를 반환한다.
     *
     * @param teamId 팀 ID
     * @return 멤버 수
     */
    public long countMembers(Long teamId) {
        return teamMemberRepository.countByTeamId(teamId);
    }
}
