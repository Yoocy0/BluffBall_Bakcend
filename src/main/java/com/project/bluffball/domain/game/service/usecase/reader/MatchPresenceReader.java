package com.project.bluffball.domain.game.service.usecase.reader;

import com.project.bluffball.domain.game.redis.MatchPresence;
import com.project.bluffball.domain.game.repository.MatchPresenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MatchPresence Redis 읽기 전담 Reader.
 */
@Component
@RequiredArgsConstructor
public class MatchPresenceReader {

    private final MatchPresenceRepository matchPresenceRepository;

    public Optional<MatchPresence> findById(String matchSessionId) {
        return matchPresenceRepository.findById(matchSessionId)
                .map(this::ensureInitialized);
    }

    public MatchPresence getById(String matchSessionId) {
        return findById(matchSessionId)
                .orElseGet(() -> new MatchPresence(matchSessionId, List.of()));
    }

    private MatchPresence ensureInitialized(MatchPresence presence) {
        presence.ensureParticipantsInitialized();
        return presence;
    }
}
