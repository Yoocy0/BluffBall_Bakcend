package com.project.bluffball.domain.card.dto.request;

import com.project.bluffball.domain.card.enums.TimingEnhancement;
import jakarta.validation.constraints.NotNull;

/**
 * 구종 타이밍 강화 요청.
 */
public record EnhanceTimingRequest(

        /** FASTER 또는 SLOWER (NONE 불가) */
        @NotNull
        TimingEnhancement timingEnhancement
) {
}
