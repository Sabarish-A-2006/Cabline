package com.sclaria.cabbooking.api.dto;

import java.math.BigDecimal;

public record FareQuoteView(BigDecimal distanceKm, Long durationSeconds, BigDecimal grossFare,
                            BigDecimal discountAmount, BigDecimal estimatedFare, String promoCode,
                            String currency, String polyline, boolean liveRoute) {}
