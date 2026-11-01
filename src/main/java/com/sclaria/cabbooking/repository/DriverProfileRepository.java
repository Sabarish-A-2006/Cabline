package com.sclaria.cabbooking.repository;

import com.sclaria.cabbooking.domain.DriverProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {
    Optional<DriverProfile> findByUserId(UUID userId);
    List<DriverProfile> findByAvailableTrue();
    long countByAvailableTrue();
}
