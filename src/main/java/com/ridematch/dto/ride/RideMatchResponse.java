package com.ridematch.dto.ride;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.ridematch.domain.enums.DestinationMatchType;

/** Ranked ride result with compatibility explanation and remaining-seat count. */
public record RideMatchResponse(Long rideId, String pickupLocation, String destination,
        LocalDate travelDate, LocalTime departureTime, LocalTime departureTimeEnd,
        int passengerCapacity, int availableSeats,
        BigDecimal estimatedTotalFare, String notes, DestinationMatchType destinationMatchType,
        long departureDifferenceMinutes, int compatibilityScore, String compatibilityReason) {
}
