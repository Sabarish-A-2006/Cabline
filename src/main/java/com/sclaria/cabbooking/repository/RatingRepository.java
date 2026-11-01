package com.sclaria.cabbooking.repository;

import com.sclaria.cabbooking.domain.Rating;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
    boolean existsByRideId(UUID rideId);
}
