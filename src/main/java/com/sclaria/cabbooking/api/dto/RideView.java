package com.sclaria.cabbooking.api.dto;

import com.sclaria.cabbooking.domain.Ride;
import com.sclaria.cabbooking.domain.RideStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RideView(
        UUID id,
        UUID riderId,
        String riderName,
        UUID driverId,
        String driverName,
        String pickup,
        String dropoff,
        BigDecimal distanceKm,
        BigDecimal estimatedFare,
        BigDecimal finalFare,
        String currency,
        RideStatus status,
        Instant createdAt,
        Instant updatedAt,
        BigDecimal discountAmount,
        String promoCode,
        Long routeDurationSeconds,
        String routePolyline) {
    public static RideView from(Ride ride, String currency) {
        return new RideView(
                ride.getId(), ride.getRider().getId(), ride.getRider().getName(),
                ride.getDriver() == null ? null : ride.getDriver().getId(),
                ride.getDriver() == null ? null : ride.getDriver().getName(),
                ride.getPickup(), ride.getDropoff(), ride.getDistanceKm(),
                ride.getEstimatedFare(), ride.getFinalFare(), currency, ride.getStatus(),
                ride.getCreatedAt(), ride.getUpdatedAt(), ride.getDiscountAmount(), ride.getPromoCode(),
                ride.getRouteDurationSeconds(), ride.getRoutePolyline());
    }
}
