package com.sclaria.cabbooking.service;

import com.sclaria.cabbooking.config.FareProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class FareCalculator {
    private final FareProperties properties;

    public FareCalculator(FareProperties properties) { this.properties = properties; }

    public BigDecimal estimate(BigDecimal distanceKm) {
        return estimate(distanceKm, BigDecimal.ZERO);
    }

    public BigDecimal estimate(BigDecimal distanceKm, BigDecimal discountPercent) {
        BigDecimal gross = properties.getBaseFare()
                .add(distanceKm.multiply(properties.getPerKilometer()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = discountAmount(gross, discountPercent);
        return gross.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal discountAmount(BigDecimal gross, BigDecimal discountPercent) {
        if (discountPercent == null || discountPercent.signum() <= 0) return BigDecimal.ZERO.setScale(2);
        return gross.multiply(discountPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
