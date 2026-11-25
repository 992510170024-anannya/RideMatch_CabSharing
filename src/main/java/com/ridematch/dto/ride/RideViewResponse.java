package com.ridematch.dto.ride;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.ridematch.domain.entity.Ride;
import com.ridematch.domain.enums.RideStatus;

/** Safe representation of a created ride. */
public record RideViewResponse(Long id, Long communityId, String pickupLocation, String destination,
        LocalDate travelDate, LocalTime departureTime, LocalTime departureTimeEnd,
        int passengerCapacity, int availableSeats,
        BigDecimal estimatedTotalFare, String notes, RideStatus status) {

    public static RideViewResponse from(Ride ride, int activePassengers) {
        return new RideViewResponse(ride.getId(), ride.getCommunity().getId(), ride.getOrigin(),
                ride.getDestination(), ride.getRequestedTime().toLocalDate(), ride.getRequestedTime().toLocalTime(),
                ride.getRequestedTimeEnd() == null ? null : ride.getRequestedTimeEnd().toLocalTime(),
                ride.getPassengerCapacity(), Math.max(0, ride.getPassengerCapacity() - activePassengers),
                ride.getEstimatedTotalFare(), ride.getNotes(), ride.getStatus());
    }
}
