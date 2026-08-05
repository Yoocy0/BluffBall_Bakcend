package com.project.bluffball.domain.user.service.usecase.reader;

import com.project.bluffball.domain.user.dto.response.BatterRecordResponse;
import com.project.bluffball.domain.user.dto.response.PitcherRecordResponse;
import com.project.bluffball.domain.user.dto.response.UserRecordsResponse;
import com.project.bluffball.domain.user.record.entity.BatterRecord;
import com.project.bluffball.domain.user.record.entity.PitcherRecord;
import com.project.bluffball.domain.user.record.repository.BatterRecordRepository;
import com.project.bluffball.domain.user.record.repository.PitcherRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 유저 전적(타자·투수) 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class UserRecordReader {

    /** 타자 성적 Repository */
    private final BatterRecordRepository batterRecordRepository;

    /** 투수 성적 Repository */
    private final PitcherRecordRepository pitcherRecordRepository;

    /** 유저 기본 정보 Reader */
    private final UserReader userReader;

    /**
     * 유저의 타자·투수 전적 DTO를 반환한다. (Service ✅)
     *
     * @param userId 조회 대상 유저 ID
     * @return 전적 응답
     */
    public UserRecordsResponse getRecordsResponse(Long userId) {
        String nickname = userReader.getNickname(userId);
        List<BatterRecordResponse> batterRecords = batterRecordRepository.findByUserId(userId).stream()
                .map(this::toBatterResponse)
                .toList();
        List<PitcherRecordResponse> pitcherRecords = pitcherRecordRepository.findByUserId(userId).stream()
                .map(this::toPitcherResponse)
                .toList();
        return new UserRecordsResponse(userId, nickname, batterRecords, pitcherRecords);
    }

    /**
     * 타자 Entity → DTO.
     *
     * @param record 타자 기록
     * @return 타자 성적 응답
     */
    private BatterRecordResponse toBatterResponse(BatterRecord record) {
        return new BatterRecordResponse(
                record.getId(),
                record.getGameMode(),
                record.getLeagueId(),
                record.getTotalGames(),
                record.getWins(),
                record.getLoses(),
                record.getPlateAppearances(),
                record.getAtBats(),
                record.getHits(),
                record.getBaseOnBalls(),
                record.getDoubles(),
                record.getTriples(),
                record.getHomeRuns(),
                record.getRunsBattedIn(),
                record.getStrikeOuts(),
                record.getDoublePlays(),
                calculateOps(record));
    }

    /**
     * 투수 Entity → DTO.
     *
     * @param record 투수 기록
     * @return 투수 성적 응답
     */
    private PitcherRecordResponse toPitcherResponse(PitcherRecord record) {
        return new PitcherRecordResponse(
                record.getId(),
                record.getGameMode(),
                record.getLeagueId(),
                record.getTotalGames(),
                record.getWins(),
                record.getLoses(),
                record.getInnings(),
                record.getOutCounts(),
                record.getStrikeOuts(),
                record.getBaseOnBalls(),
                record.getHits(),
                record.getEarnedRuns(),
                record.getDoublesAllowed(),
                record.getTriplesAllowed(),
                record.getHomeRunsAllowed(),
                record.getWildPitches(),
                calculateEra(record));
    }

    /**
     * 표시용 OPS를 계산한다. 타석·타수가 0이면 0.0.
     *
     * @param record 타자 기록
     * @return OPS
     */
    private double calculateOps(BatterRecord record) {
        double onBasePercentage = record.getPlateAppearances() == 0
                ? 0.0
                : (double) (record.getHits() + record.getBaseOnBalls()) / record.getPlateAppearances();
        int singles = record.getHits() - record.getDoubles() - record.getTriples() - record.getHomeRuns();
        int totalBases = singles
                + (2 * record.getDoubles())
                + (3 * record.getTriples())
                + (4 * record.getHomeRuns());
        double sluggingPercentage = record.getAtBats() == 0
                ? 0.0
                : (double) totalBases / record.getAtBats();
        return onBasePercentage + sluggingPercentage;
    }

    /**
     * 표시용 ERA를 계산한다. 소화 이닝이 0이면 0.0.
     *
     * @param record 투수 기록
     * @return ERA
     */
    private double calculateEra(PitcherRecord record) {
        double inningsPitched = record.getInnings() + (record.getOutCounts() / 3.0);
        if (inningsPitched <= 0.0) {
            return 0.0;
        }
        return (record.getEarnedRuns() * 9.0) / inningsPitched;
    }
}
