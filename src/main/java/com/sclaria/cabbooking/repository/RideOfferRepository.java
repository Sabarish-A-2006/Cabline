package com.sclaria.cabbooking.repository;

import com.sclaria.cabbooking.domain.OfferStatus;
import com.sclaria.cabbooking.domain.RideOffer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideOfferRepository extends JpaRepository<RideOffer, UUID> {
    List<RideOffer> findByDriverIdAndStatusOrderByCreatedAtDesc(UUID driverId, OfferStatus status);
    List<RideOffer> findByRideIdAndStatus(UUID rideId, OfferStatus status);
    Optional<RideOffer> findByIdAndDriverId(UUID id, UUID driverId);
    long countByRideIdAndStatus(UUID rideId, OfferStatus status);
}
