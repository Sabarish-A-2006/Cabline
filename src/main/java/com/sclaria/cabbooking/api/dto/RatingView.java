package com.sclaria.cabbooking.api.dto;

import com.sclaria.cabbooking.domain.Rating;
import java.time.Instant;
import java.util.UUID;

public record RatingView(UUID id, UUID rideId, int stars, String comment, Instant createdAt) {
    public static RatingView from(Rating rating) {
        return new RatingView(rating.getId(), rating.getRide().getId(), rating.getStars(),
                rating.getComment(), rating.getCreatedAt());
    }
}
