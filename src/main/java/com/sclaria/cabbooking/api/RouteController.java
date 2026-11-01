package com.sclaria.cabbooking.api;

import com.sclaria.cabbooking.api.dto.BookingRequest;
import com.sclaria.cabbooking.api.dto.FareQuoteView;
import com.sclaria.cabbooking.config.FareProperties;
import com.sclaria.cabbooking.service.FareCalculator;
import com.sclaria.cabbooking.service.RouteEstimateService;
import com.sclaria.cabbooking.repository.PromoCodeRepository;
import com.sclaria.cabbooking.domain.PromoCode;
import com.sclaria.cabbooking.error.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
@PreAuthorize("hasRole('RIDER')")
public class RouteController {
    private final RouteEstimateService routes;
    private final FareCalculator fares;
    private final FareProperties properties;
    private final PromoCodeRepository promos;

    public RouteController(RouteEstimateService routes, FareCalculator fares, FareProperties properties,
                           PromoCodeRepository promos) {
        this.routes = routes;
        this.fares = fares;
        this.properties = properties;
        this.promos = promos;
    }

    @PostMapping("/estimate")
    public FareQuoteView estimate(@Valid @RequestBody BookingRequest request) {
        var route = routes.estimate(request.pickup(), request.dropoff(), request.distanceKm());
        BigDecimal gross = fares.estimate(route.distanceKm());
        PromoCode promo = findPromo(request.promoCode());
        BigDecimal percent = promo == null ? BigDecimal.ZERO : promo.getDiscountPercent();
        BigDecimal discount = fares.discountAmount(gross, percent);
        return new FareQuoteView(route.distanceKm(), route.durationSeconds(), gross, discount,
                gross.subtract(discount).max(BigDecimal.ZERO), promo == null ? null : promo.getCode(),
                properties.getCurrency(), route.polyline(), route.liveRoute());
    }

    private PromoCode findPromo(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) return null;
        PromoCode promo = promos.findByCodeIgnoreCase(rawCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));
        promo.requireUsable(Instant.now());
        return promo;
    }
}
