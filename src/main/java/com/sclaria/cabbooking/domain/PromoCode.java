package com.sclaria.cabbooking.domain;

import com.sclaria.cabbooking.error.DomainConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promo_codes")
public class PromoCode {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(nullable = false)
    private Instant expiresAt;

    private Integer maxUses;

    @Column(nullable = false)
    private int usageCount;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private long version;

    protected PromoCode() {}

    public PromoCode(String code, BigDecimal discountPercent, Instant expiresAt, Integer maxUses) {
        this.code = code;
        this.discountPercent = discountPercent;
        this.expiresAt = expiresAt;
        this.maxUses = maxUses;
    }

    public void requireUsable(Instant now) {
        if (!active || !expiresAt.isAfter(now) || (maxUses != null && usageCount >= maxUses)) {
            throw new DomainConflictException("Promo code is expired or no longer available");
        }
    }

    public void recordUse() { usageCount++; }
    public void setActive(boolean active) { this.active = active; }
    public UUID getId() { return id; }
    public String getCode() { return code; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public Instant getExpiresAt() { return expiresAt; }
    public Integer getMaxUses() { return maxUses; }
    public int getUsageCount() { return usageCount; }
    public boolean isActive() { return active; }
}
