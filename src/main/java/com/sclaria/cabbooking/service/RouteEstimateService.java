package com.sclaria.cabbooking.service;

import com.sclaria.cabbooking.api.dto.RouteEstimateView;
import com.sclaria.cabbooking.error.DomainConflictException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class RouteEstimateService {
    private final String apiKey;
    private final RestClient client = RestClient.create();

    public RouteEstimateService(@Value("${app.maps.api-key:}") String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public RouteEstimateView estimate(String pickup, String dropoff, BigDecimal fallbackDistanceKm) {
        if (apiKey.isBlank()) {
            if (fallbackDistanceKm == null) {
                throw new DomainConflictException("Provide distanceKm or configure GOOGLE_MAPS_API_KEY");
            }
            return new RouteEstimateView(fallbackDistanceKm.setScale(2, RoundingMode.HALF_UP), null, null, false);
        }
        try {
            Map<String, Object> body = Map.of(
                    "origin", Map.of("address", pickup),
                    "destination", Map.of("address", dropoff),
                    "travelMode", "DRIVE",
                    "routingPreference", "TRAFFIC_AWARE");
            Map<?, ?> response = client.post()
                    .uri("https://routes.googleapis.com/directions/v2:computeRoutes")
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response == null || !(response.get("routes") instanceof List<?> routes) || routes.isEmpty()) {
                throw new DomainConflictException("No driving route was found for these locations");
            }
            Map<?, ?> route = (Map<?, ?>) routes.get(0);
            BigDecimal meters = new BigDecimal(String.valueOf(route.get("distanceMeters")));
            BigDecimal km = meters.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
            Long durationSeconds = parseDuration(String.valueOf(route.get("duration")));
            String polyline = null;
            if (route.get("polyline") instanceof Map<?, ?> polylineData) {
                polyline = String.valueOf(polylineData.get("encodedPolyline"));
            }
            return new RouteEstimateView(km, durationSeconds, polyline, true);
        } catch (RestClientException | NumberFormatException exception) {
            throw new DomainConflictException("Google Maps route estimate is unavailable right now");
        }
    }

    private Long parseDuration(String duration) {
        if (duration == null || !duration.endsWith("s")) return null;
        try { return new BigDecimal(duration.substring(0, duration.length() - 1)).longValue(); }
        catch (NumberFormatException exception) { return null; }
    }
}
