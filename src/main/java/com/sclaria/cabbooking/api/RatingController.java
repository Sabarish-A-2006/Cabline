package com.sclaria.cabbooking.api;

import com.sclaria.cabbooking.api.dto.RatingRequest;
import com.sclaria.cabbooking.api.dto.RatingView;
import com.sclaria.cabbooking.service.RatingService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rides/{rideId}/rating")
@PreAuthorize("hasRole('RIDER')")
public class RatingController {
    private final RatingService ratings;

    public RatingController(RatingService ratings) { this.ratings = ratings; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RatingView rate(Authentication authentication, @PathVariable UUID rideId,
                           @Valid @RequestBody RatingRequest request) {
        return ratings.rate(authentication, rideId, request);
    }
}
