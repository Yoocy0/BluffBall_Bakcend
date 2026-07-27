package com.project.bluffball.domain.team.service.usecase.reader;

import com.project.bluffball.domain.league.dto.response.LeagueResponse;
import com.project.bluffball.domain.league.entity.TeamLeagueProgress;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.repository.TeamLeagueProgressRepository;
import com.project.bluffball.domain.league.service.usecase.reader.LeagueReader;
import com.project.bluffball.domain.team.dto.response.TeamRecordAggregateResponse;
import com.project.bluffball.domain.team.dto.response.TeamRecordItemResponse;
import com.project.bluffball.domain.team.dto.response.TeamRecordsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 팀 리그 기록 읽기 전담 Reader (usecase/reader 계층).
 *
 * <p>상시 티어 진행({@link TeamLeagueProgress}) 기준이다.</p>
 */
@Component
@RequiredArgsConstructor
public class TeamRecordReader {

    private final TeamLeagueProgressRepository teamLeagueProgressRepository;
    private final LeagueReader leagueReader;

    /**
     * 필터·합산 조건에 맞는 팀 리그 기록을 반환한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분 필터 (nullable)
     * @param tier 리그 단계 필터 (nullable)
     * @param aggregate true면 합산 결과만 반환
     * @return 기록 응답 DTO
     */
    public TeamRecordsResponse getRecords(
            Long teamId,
            LeagueFormat format,
            LeagueTier tier,
            boolean aggregate) {

        List<TeamRecordItemResponse> records = loadFilteredRecords(teamId, format, tier);

        if (aggregate) {
            TeamRecordAggregateResponse aggregateResponse = sum(records);
            return new TeamRecordsResponse(List.of(), aggregateResponse);
        }
        return new TeamRecordsResponse(records, null);
    }

    /**
     * 필터에 맞는 기록 목록을 로드한다.
     *
     * @param teamId 팀 ID
     * @param format 포맷 필터
     * @param tier 티어 필터
     * @return 기록 항목 목록
     */
    private List<TeamRecordItemResponse> loadFilteredRecords(
            Long teamId,
            LeagueFormat format,
            LeagueTier tier) {

        List<TeamLeagueProgress> progresses = teamLeagueProgressRepository.findByTeamId(teamId);
        List<TeamRecordItemResponse> result = new ArrayList<>();
        for (TeamLeagueProgress progress : progresses) {
            if (format != null && progress.getFormat() != format) {
                continue;
            }
            if (tier != null && progress.getCurrentTier() != tier) {
                continue;
            }
            result.add(toItem(progress));
        }
        return result;
    }

    /**
     * Progress → 기록 항목 변환.
     *
     * @param progress 진행 상태
     * @return 기록 항목
     */
    private TeamRecordItemResponse toItem(TeamLeagueProgress progress) {
        LeagueResponse league = leagueReader.getByFormatAndTier(
                progress.getFormat(), progress.getCurrentTier());
        return new TeamRecordItemResponse(
                league.leagueId(),
                progress.getFormat(),
                progress.getCurrentTier(),
                progress.getWins(),
                progress.getLoses(),
                progress.getRunDiff(),
                progress.getRating());
    }

    /**
     * 기록 목록을 승/패/득실로 합산한다.
     *
     * @param records 기록 목록
     * @return 합산 응답
     */
    private TeamRecordAggregateResponse sum(List<TeamRecordItemResponse> records) {
        int wins = 0;
        int losses = 0;
        int runDiff = 0;
        for (TeamRecordItemResponse record : records) {
            wins += record.wins();
            losses += record.losses();
            runDiff += record.runDiff();
        }
        return new TeamRecordAggregateResponse(wins, losses, runDiff, records.size());
    }
}
