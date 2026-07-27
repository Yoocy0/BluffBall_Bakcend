package com.project.bluffball.domain.league.service.usecase.reader;

import com.project.bluffball.domain.league.dto.response.LeagueResponse;
import com.project.bluffball.domain.league.entity.League;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.repository.LeagueRepository;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 리그 카탈로그 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class LeagueReader {

    private final LeagueRepository leagueRepository;

    /**
     * 리그 Entity를 조회한다. Executor·Reader 내부 전용 (Service에서 호출 금지).
     *
     * @param leagueId 리그 ID
     * @return 리그 Entity
     * @throws NotFoundException 리그가 없으면
     */
    public League getById(Long leagueId) {
        return leagueRepository.findById(leagueId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.LEAGUE_NOT_FOUND, "leagueId=" + leagueId));
    }

    /**
     * 리그 상세 DTO를 반환한다.
     *
     * @param leagueId 리그 ID
     * @return 리그 응답 DTO
     */
    public LeagueResponse getLeagueResponse(Long leagueId) {
        return toResponse(getById(leagueId));
    }

    /**
     * 리그 카탈로그 목록을 반환한다.
     *
     * @param format 포맷 필터 (nullable)
     * @param tier 티어 필터 (nullable)
     * @return 리그 응답 목록
     */
    public List<LeagueResponse> getLeagues(LeagueFormat format, LeagueTier tier) {
        List<League> leagues;
        if (format != null && tier != null) {
            leagues = leagueRepository.findByFormatAndTier(format, tier)
                    .map(List::of)
                    .orElseGet(List::of);
        } else if (format != null) {
            leagues = leagueRepository.findByFormat(format);
        } else if (tier != null) {
            leagues = leagueRepository.findByTier(tier);
        } else {
            leagues = leagueRepository.findAll();
        }
        return leagues.stream().map(this::toResponse).toList();
    }

    /**
     * 포맷·티어로 리그 응답을 반환한다.
     *
     * @param format 포맷
     * @param tier 티어
     * @return 리그 응답
     */
    public LeagueResponse getByFormatAndTier(LeagueFormat format, LeagueTier tier) {
        return leagueRepository.findByFormatAndTier(format, tier)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.LEAGUE_NOT_FOUND, "format=" + format + ", tier=" + tier));
    }

    /**
     * 참여권 가격을 반환한다.
     *
     * @param leagueId 리그 ID
     * @return 참여권 가격
     */
    public long getEntryFee(Long leagueId) {
        return getById(leagueId).getEntryFee();
    }

    /**
     * 최소 팀원 수를 반환한다.
     *
     * @param leagueId 리그 ID
     * @return 최소 팀원 수
     */
    public int getMinTeamMembers(Long leagueId) {
        return getById(leagueId).getMinTeamMembers();
    }

    /**
     * Entity → 응답 DTO 변환.
     *
     * @param league 리그 Entity
     * @return 응답 DTO
     */
    private LeagueResponse toResponse(League league) {
        return new LeagueResponse(
                league.getId(),
                league.getFormat(),
                league.getTier(),
                league.getName(),
                league.getEntryFee(),
                league.getFirstPlacePrize());
    }
}
