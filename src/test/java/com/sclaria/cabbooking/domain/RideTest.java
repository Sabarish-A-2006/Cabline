package com.sclaria.cabbooking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sclaria.cabbooking.error.DomainConflictException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RideTest {
    private final AppUser rider = new AppUser("Rider", "rider@example.test", "hash", UserRole.RIDER);
    private final AppUser driver = new AppUser("Driver", "driver@example.test", "hash", UserRole.DRIVER);

    @Test
    void followsRequestedAcceptedStartedCompletedFlow() {
        Ride ride = new Ride(rider, "Pickup", "Dropoff", new BigDecimal("5.00"), new BigDecimal("140.00"));

        ride.accept(driver);
        assertThat(ride.getStatus()).isEqualTo(RideStatus.ACCEPTED);
        ride.start(driver);
        assertThat(ride.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
        ride.complete(driver);

        assertThat(ride.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(ride.getFinalFare()).isEqualByComparingTo("140.00");
    }

    @Test
    void preventsStartingARequestedRide() {
        Ride ride = new Ride(rider, "Pickup", "Dropoff", new BigDecimal("5.00"), new BigDecimal("140.00"));

        assertThatThrownBy(() -> ride.start(driver)).isInstanceOf(DomainConflictException.class);
    }
}
