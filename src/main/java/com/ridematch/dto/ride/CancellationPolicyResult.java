package com.ridematch.dto.ride;

import com.ridematch.domain.enums.CancellationPolicyDecision;

/** Human-readable, non-monetary result from the cancellation policy service. */
public record CancellationPolicyResult(CancellationPolicyDecision decision, String explanation) {
}
