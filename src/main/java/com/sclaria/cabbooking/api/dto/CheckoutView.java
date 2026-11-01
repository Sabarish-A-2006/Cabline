package com.sclaria.cabbooking.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutView(UUID paymentId, String mode, BigDecimal amount, String currency,
                           String clientSecret, String publishableKey) {}
