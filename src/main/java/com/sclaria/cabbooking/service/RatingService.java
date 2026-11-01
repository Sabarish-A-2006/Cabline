package com.sclaria.cabbooking.service;

import com.sclaria.cabbooking.api.dto.RatingRequest;
import com.sclaria.cabbooking.api.dto.RatingView;
import com.sclaria.cabbooking.domain.Rating;
import com.sclaria.cabbooking.domain.RideStatus;
import com.sclaria.cabbooking.domain.UserRole;
import com.sclaria.cabbooking.error.DomainConflictException;
import com.sclaria.cabbooking.error.ResourceNotFoundException;
import com.sclaria.cabbooking.repository.RatingRepository;
import com.sclaria.cabbooking.repository.RideRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {
    private final CurrentUserService currentUser;
    private final RideRepository rides;
    private final RatingRepository ratings;

    public RatingService(CurrentUserService currentUser, RideRepository rides, RatingRepository ratings) {
        this.currentUser = currentUser;
        this.rides = rides;
        this.ratings = ratings;
    }

    @Transactional
    public RatingView rate(Authentication authentication, UUID rideId, RatingRequest request) {
        var rider = currentUser.require(authentication, UserRole.RIDER);
        var ride = rides.findById(rideId).orElseThrow(() -> new ResourceNotFoundException("Ride not found"));
        if (!ride.getRider().getId().equals(rider.getId())) throw new DomainConflictException("Only the rider can rate this ride");
        if (ride.getStatus() != RideStatus.COMPLETED) throw new DomainConflictException("Only completed rides can be rated");
        if (ratings.existsByRideId(rideId)) throw new DomainConflictException("This ride has already been rated");
        String comment = request.comment() == null || request.comment().isBlank() ? null : request.comment().trim();
        return RatingView.from(ratings.save(new Rating(ride, request.stars(), comment)));
    }
}
