package com.ridematch.dto.ride;

import java.math.BigDecimal;

/** Equal estimated share based on confirmed passengers only. */
public record FareSplitResponse(BigDecimal estimatedTotalFare, int confirmedPassengerCount,
        BigDecimal estimatedContributionEach, String explanation) {
}
