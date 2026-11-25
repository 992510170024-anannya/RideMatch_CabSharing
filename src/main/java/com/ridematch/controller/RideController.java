package com.ridematch.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.ridematch.dto.ride.RideCreateRequest;
import com.ridematch.dto.ride.RideSearchRequest;
import com.ridematch.service.RideService;
import java.time.LocalDateTime;

import jakarta.validation.Valid;

/** Web forms for creating rides and searching ranked compatible rides. */
@Controller
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @GetMapping("/communities/{communityId}/rides/new")
    public String createRidePage(@PathVariable Long communityId, Authentication authentication, Model model) {
        model.addAttribute("community", rideService.requireRideAccess(authentication.getName(), communityId));
        model.addAttribute("rideCreateRequest", new RideCreateRequest());
        return "ride/create";
    }

    @PostMapping("/communities/{communityId}/rides")
    public String createRide(@PathVariable Long communityId, Authentication authentication,
            @Valid @ModelAttribute("rideCreateRequest") RideCreateRequest request,
            BindingResult bindingResult, Model model) {
        if (isPastDeparture(request.getTravelDate(), request.getDepartureTime())) {
            bindingResult.addError(new FieldError("rideCreateRequest", "departureTime",
                    "Departure must be a future date and time"));
        }
        if (request.getDepartureTimeEnd() != null && request.getDepartureTime() != null
                && request.getDepartureTimeEnd().isBefore(request.getDepartureTime())) {
            bindingResult.addError(new FieldError("rideCreateRequest", "departureTimeEnd",
                    "Latest departure must be the same time or later than the start time"));
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("community", rideService.requireRideAccess(authentication.getName(), communityId));
            return "ride/create";
        }
        model.addAttribute("createdRide",
                rideService.createRide(authentication.getName(), communityId, request));
        model.addAttribute("community", rideService.requireRideAccess(authentication.getName(), communityId));
        return "ride/created";
    }

    @GetMapping("/rides/search")
    public String searchPage(Authentication authentication, Model model) {
        model.addAttribute("verifiedCommunities", rideService.listVerifiedCommunities(authentication.getName()));
        model.addAttribute("rideSearchRequest", new RideSearchRequest());
        return "ride/search";
    }

    @PostMapping("/rides/search")
    public String searchRides(Authentication authentication,
            @Valid @ModelAttribute("rideSearchRequest") RideSearchRequest request,
            BindingResult bindingResult, Model model) {
        model.addAttribute("verifiedCommunities", rideService.listVerifiedCommunities(authentication.getName()));
        if (isPastDeparture(request.getTravelDate(), request.getDepartureTime())) {
            bindingResult.addError(new FieldError("rideSearchRequest", "departureTime",
                    "Search departure must be a future date and time"));
        }
        if (bindingResult.hasErrors()) {
            return "ride/search";
        }
        model.addAttribute("matches", rideService.searchRides(authentication.getName(), request));
        return "ride/results";
    }

    private static boolean isPastDeparture(java.time.LocalDate date, java.time.LocalTime time) {
        return date != null && time != null && !LocalDateTime.of(date, time).isAfter(LocalDateTime.now());
    }
}
