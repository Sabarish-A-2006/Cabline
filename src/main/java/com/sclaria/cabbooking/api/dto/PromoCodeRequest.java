package com.sclaria.cabbooking.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record PromoCodeRequest(
        @NotBlank @Size(min = 3, max = 30) String code,
        @NotNull @DecimalMin("1.00") @DecimalMax("100.00") BigDecimal discountPercent,
        @NotNull @Future Instant expiresAt,
        @Min(1) Integer maxUses) {}
