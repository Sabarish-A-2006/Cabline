package com.sclaria.cabbooking.api;

import com.sclaria.cabbooking.api.dto.BookingRequest;
import com.sclaria.cabbooking.api.dto.OfferView;
import com.sclaria.cabbooking.api.dto.RideView;
import com.sclaria.cabbooking.service.RideService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RideController {
    private final RideService rides;

    public RideController(RideService rides) { this.rides = rides; }

    @PostMapping("/rides")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RIDER')")
    public RideView book(Authentication authentication, @Valid @RequestBody BookingRequest request) {
        return rides.book(authentication, request);
    }

    @GetMapping("/rides/me")
    @PreAuthorize("hasAnyRole('RIDER', 'DRIVER')")
    public List<RideView> myRides(Authentication authentication) { return rides.myRides(authentication); }

    @PostMapping("/rides/{rideId}/cancel")
    @PreAuthorize("hasRole('RIDER')")
    public RideView cancel(Authentication authentication, @PathVariable UUID rideId) {
        return rides.cancel(authentication, rideId);
    }

    @GetMapping("/drivers/me/offers")
    @PreAuthorize("hasRole('DRIVER')")
    public List<OfferView> myOffers(Authentication authentication) { return rides.myPendingOffers(authentication); }

    @PostMapping("/driver-offers/{offerId}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public RideView accept(Authentication authentication, @PathVariable UUID offerId) {
        return rides.respondToOffer(authentication, offerId, true);
    }

    @PostMapping("/driver-offers/{offerId}/decline")
    @PreAuthorize("hasRole('DRIVER')")
    public RideView decline(Authentication authentication, @PathVariable UUID offerId) {
        return rides.respondToOffer(authentication, offerId, false);
    }

    @PostMapping("/rides/{rideId}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public RideView start(Authentication authentication, @PathVariable UUID rideId) {
        return rides.start(authentication, rideId);
    }

    @PostMapping("/rides/{rideId}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public RideView complete(Authentication authentication, @PathVariable UUID rideId) {
        return rides.complete(authentication, rideId);
    }
}
