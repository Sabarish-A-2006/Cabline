package com.sclaria.cabbooking.api.dto;

import com.sclaria.cabbooking.domain.Payment;
import com.sclaria.cabbooking.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentView(UUID id, UUID rideId, BigDecimal amount, String currency, PaymentStatus status, Instant createdAt,
                          String receiptUrl) {
    public static PaymentView from(Payment payment) {
        return new PaymentView(payment.getId(), payment.getRide().getId(), payment.getAmount(),
                payment.getCurrency(), payment.getStatus(), payment.getCreatedAt(), "/api/payments/" + payment.getId() + "/receipt");
    }
}
