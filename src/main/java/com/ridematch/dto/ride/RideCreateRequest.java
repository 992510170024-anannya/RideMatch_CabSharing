package com.ridematch.dto.ride;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Validated ride creation form; community and creator come from the authenticated route. */
public class RideCreateRequest {

    @NotBlank(message = "Pickup location is required")
    @Size(max = 180, message = "Pickup location must be at most 180 characters")
    private String pickupLocation;

    @NotBlank(message = "Destination is required")
    @Size(max = 180, message = "Destination must be at most 180 characters")
    private String destination;

    @NotNull(message = "Travel date is required")
    @FutureOrPresent(message = "Travel date cannot be in the past")
    private LocalDate travelDate;

    @NotNull(message = "Departure time is required")
    private LocalTime departureTime;

    private LocalTime departureTimeEnd;

    @NotNull(message = "Passenger capacity is required")
    @Min(value = 1, message = "Passenger capacity must be at least 1")
    private Integer passengerCapacity;

    @NotNull(message = "Estimated fare is required")
    @DecimalMin(value = "0.01", message = "Estimated fare must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Estimated fare can have at most 8 whole digits and 2 decimal places")
    private BigDecimal estimatedTotalFare;

    @Size(max = 1000, message = "Notes must be at most 1000 characters")
    private String notes;

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }
    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }
    public LocalTime getDepartureTimeEnd() { return departureTimeEnd; }
    public void setDepartureTimeEnd(LocalTime departureTimeEnd) { this.departureTimeEnd = departureTimeEnd; }
    public Integer getPassengerCapacity() { return passengerCapacity; }
    public void setPassengerCapacity(Integer passengerCapacity) { this.passengerCapacity = passengerCapacity; }
    public BigDecimal getEstimatedTotalFare() { return estimatedTotalFare; }
    public void setEstimatedTotalFare(BigDecimal estimatedTotalFare) { this.estimatedTotalFare = estimatedTotalFare; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
