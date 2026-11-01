package com.sclaria.cabbooking.domain;

import com.sclaria.cabbooking.error.DomainConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false, unique = true)
    private Ride ride;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(unique = true, length = 120)
    private String stripePaymentIntentId;

    @Column(length = 200)
    private String stripeClientSecret;

    protected Payment() {}

    public Payment(Ride ride, BigDecimal amount, String currency) {
        this.ride = ride;
        this.amount = amount;
        this.currency = currency;
    }

    public void markPaid() {
        if (status != PaymentStatus.PENDING) throw new DomainConflictException("Payment is already settled");
        status = PaymentStatus.PAID;
    }

    public void setStripeIntent(String intentId, String clientSecret) {
        this.stripePaymentIntentId = intentId;
        this.stripeClientSecret = clientSecret;
    }

    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public String getStripeClientSecret() { return stripeClientSecret; }

    public UUID getId() { return id; }
    public Ride getRide() { return ride; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
