package com.sclaria.cabbooking.api.dto;

import com.sclaria.cabbooking.domain.RideOffer;
import com.sclaria.cabbooking.domain.OfferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OfferView(
        UUID offerId,
        UUID rideId,
        String pickup,
        String dropoff,
        BigDecimal distanceKm,
        BigDecimal estimatedFare,
        String currency,
        OfferStatus status,
        Instant createdAt) {
    public static OfferView from(RideOffer offer, String currency) {
        var ride = offer.getRide();
        return new OfferView(offer.getId(), ride.getId(), ride.getPickup(), ride.getDropoff(),
                ride.getDistanceKm(), ride.getEstimatedFare(), currency, offer.getStatus(), offer.getCreatedAt());
    }
}
