package com.sclaria.cabbooking.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BookingRequest(
        @NotBlank @Size(max = 200) String pickup,
        @NotBlank @Size(max = 200) String dropoff,
        @DecimalMin(value = "0.10") @DecimalMax(value = "500.00") BigDecimal distanceKm,
        @DecimalMin("-90") @DecimalMax("90") Double pickupLatitude,
        @DecimalMin("-180") @DecimalMax("180") Double pickupLongitude,
        @Size(max = 30) String promoCode) {}
