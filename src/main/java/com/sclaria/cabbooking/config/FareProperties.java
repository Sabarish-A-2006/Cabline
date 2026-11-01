package com.sclaria.cabbooking.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.fare")
public class FareProperties {
    private String currency = "INR";
    private BigDecimal baseFare = new BigDecimal("50.00");
    private BigDecimal perKilometer = new BigDecimal("18.00");

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }
    public BigDecimal getPerKilometer() { return perKilometer; }
    public void setPerKilometer(BigDecimal perKilometer) { this.perKilometer = perKilometer; }
}
