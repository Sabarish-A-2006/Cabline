package com.sclaria.cabbooking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sclaria.cabbooking.config.FareProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FareCalculatorTest {
    @Test
    void estimatesFareFromBaseAndDistanceRates() {
        FareProperties properties = new FareProperties();
        properties.setBaseFare(new BigDecimal("50.00"));
        properties.setPerKilometer(new BigDecimal("18.00"));

        BigDecimal fare = new FareCalculator(properties).estimate(new BigDecimal("12.50"));

        assertThat(fare).isEqualByComparingTo("275.00");
    }

    @Test
    void roundsToTwoDecimalPlaces() {
        FareProperties properties = new FareProperties();
        properties.setBaseFare(new BigDecimal("10.00"));
        properties.setPerKilometer(new BigDecimal("3.33"));

        BigDecimal fare = new FareCalculator(properties).estimate(new BigDecimal("1.25"));

        assertThat(fare).isEqualByComparingTo("14.16");
    }
}
