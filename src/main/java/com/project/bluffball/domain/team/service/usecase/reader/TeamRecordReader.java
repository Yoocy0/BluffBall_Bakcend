package com.project.bluffball.domain.team.service.usecase.reader;

import com.project.bluffball.domain.league.entity.League;
import com.project.bluffball.domain.league.entity.LeagueSeason;
import com.project.bluffball.domain.league.entity.LeagueTeam;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.repository.LeagueRepository;
import com.project.bluffball.domain.league.repository.LeagueSeasonRepository;
import com.project.bluffball.domain.league.repository.LeagueTeamRepository;
import com.project.bluffball.domain.team.dto.response.TeamRecordAggregateResponse;
import com.project.bluffball.domain.team.dto.response.TeamRecordItemResponse;
import com.project.bluffball.domain.team.dto.response.TeamRecordsResponse;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 팀 리그 기록 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamRecordReader {

    private final LeagueTeamRepository leagueTeamRepository;
    private final LeagueSeasonRepository leagueSeasonRepository;
    private final LeagueRepository leagueRepository;

    /**
     * 필터·합산 조건에 맞는 팀 리그 기록을 반환한다.
     *
     * @param teamId 팀 ID
     * @param seasonId 시즌 ID 필터 (nullable)
     * @param format 리그 구분 필터 (nullable)
     * @param tier 리그 단계 필터 (nullable)
     * @param aggregate true면 합산 결과만 반환
     * @return 기록 응답 DTO
     */
    public TeamRecordsResponse getRecords(
            Long teamId,
            Long seasonId,
            LeagueFormat format,
            LeagueTier tier,
            boolean aggregate) {

        List<TeamRecordItemResponse> records = loadFilteredRecords(teamId, seasonId, format, tier);

        if (aggregate) {
            TeamRecordAggregateResponse aggregateResponse = sum(records);
            return new TeamRecordsResponse(List.of(), aggregateResponse);
        }
        return new TeamRecordsResponse(records, null);
    }

    /**
     * 특정 시즌 기록을 반환한다.
     *
     * @param teamId 팀 ID
     * @param seasonId 시즌 ID
     * @return 시즌 기록 DTO
     * @throws NotFoundException 기록이 없으면
     */
    public TeamRecordItemResponse getSeasonRecord(Long teamId, Long seasonId) {
        LeagueTeam leagueTeam = leagueTeamRepository.findByTeamIdAndLeagueSeasonId(teamId, seasonId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.TEAM_RECORD_NOT_FOUND,
                        "teamId=" + teamId + ", seasonId=" + seasonId));
        return toItem(leagueTeam)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.TEAM_RECORD_NOT_FOUND,
                        "teamId=" + teamId + ", seasonId=" + seasonId));
    }

    /**
     * 필터에 맞는 기록 목록을 로드한다.
     *
     * @param teamId 팀 ID
     * @param seasonId 시즌 ID 필터
     * @param format 포맷 필터
     * @param tier 티어 필터
     * @return 기록 항목 목록
     */
    private List<TeamRecordItemResponse> loadFilteredRecords(
            Long teamId,
            Long seasonId,
            LeagueFormat format,
            LeagueTier tier) {

        List<LeagueTeam> leagueTeams = (seasonId != null)
                ? leagueTeamRepository.findByTeamIdAndLeagueSeasonId(teamId, seasonId)
                .map(List::of)
                .orElseGet(List::of)
                : leagueTeamRepository.findByTeamId(teamId);

        List<TeamRecordItemResponse> result = new ArrayList<>();
        for (LeagueTeam leagueTeam : leagueTeams) {
            Optional<TeamRecordItemResponse> item = toItem(leagueTeam);
            if (item.isEmpty()) {
                continue;
            }
            TeamRecordItemResponse record = item.get();
            if (format != null && record.format() != format) {
                continue;
            }
            if (tier != null && record.tier() != tier) {
                continue;
            }
            result.add(record);
        }
        return result;
    }

    /**
     * LeagueTeam → 기록 항목 변환.
     *
     * @param leagueTeam 리그 팀 Entity
     * @return 기록 항목 Optional (시즌·리그 누락 시 empty)
     */
    private Optional<TeamRecordItemResponse> toItem(LeagueTeam leagueTeam) {
        Optional<LeagueSeason> seasonOpt = leagueSeasonRepository.findById(leagueTeam.getLeagueSeasonId());
        if (seasonOpt.isEmpty()) {
            return Optional.empty();
        }
        LeagueSeason season = seasonOpt.get();
        Optional<League> leagueOpt = leagueRepository.findById(season.getLeagueId());
        if (leagueOpt.isEmpty()) {
            return Optional.empty();
        }
        League league = leagueOpt.get();
        return Optional.of(new TeamRecordItemResponse(
                season.getId(),
                league.getId(),
                league.getFormat(),
                league.getTier(),
                leagueTeam.getWins(),
                leagueTeam.getLoses(),
                leagueTeam.getRunDiff(),
                leagueTeam.getRank(),
                leagueTeam.getScore()));
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
