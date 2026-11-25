package com.ridematch.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridematch.domain.entity.Ride;
import com.ridematch.domain.enums.RideMembershipStatus;
import com.ridematch.dto.ride.FareSplitResponse;
import com.ridematch.repository.RideMembershipRepository;

/** Recalculates an equal estimate using only confirmed passengers. */
@Service
public class FareSplitService {

    private final RideMembershipRepository membershipRepository;

    public FareSplitService(RideMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public FareSplitResponse calculate(Ride ride) {
        long confirmedPassengers = membershipRepository.countByRide_IdAndStatus(
                ride.getId(), RideMembershipStatus.CONFIRMED);
        BigDecimal share = confirmedPassengers == 0
                ? BigDecimal.ZERO.setScale(2)
                : ride.getEstimatedTotalFare().divide(BigDecimal.valueOf(confirmedPassengers), 2, RoundingMode.HALF_UP);
        return new FareSplitResponse(ride.getEstimatedTotalFare(), Math.toIntExact(confirmedPassengers), share,
                "Estimated fare only. The actual taxi fare is settled outside RideMatch.");
    }
}
