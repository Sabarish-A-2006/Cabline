package com.sclaria.cabbooking.api.dto;

import java.math.BigDecimal;

public record RouteEstimateView(BigDecimal distanceKm, Long durationSeconds, String polyline, boolean liveRoute) {}
