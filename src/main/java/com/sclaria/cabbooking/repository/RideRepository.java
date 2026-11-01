package com.sclaria.cabbooking.repository;

import com.sclaria.cabbooking.domain.Ride;
import com.sclaria.cabbooking.domain.RideStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RideRepository extends JpaRepository<Ride, UUID> {
    List<Ride> findByRiderIdOrderByCreatedAtDesc(UUID riderId);
    List<Ride> findByDriverIdOrderByCreatedAtDesc(UUID driverId);
    long countByStatus(RideStatus status);
    long countByStatusIn(List<RideStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Ride r where r.id = :id")
    Optional<Ride> findByIdForUpdate(@Param("id") UUID id);
}
