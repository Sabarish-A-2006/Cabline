package com.sclaria.cabbooking.api.dto;

import com.sclaria.cabbooking.domain.PromoCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PromoCodeView(UUID id, String code, BigDecimal discountPercent, Instant expiresAt,
                            Integer maxUses, int usageCount, boolean active) {
    public static PromoCodeView from(PromoCode promo) {
        return new PromoCodeView(promo.getId(), promo.getCode(), promo.getDiscountPercent(), promo.getExpiresAt(),
                promo.getMaxUses(), promo.getUsageCount(), promo.isActive());
    }
}
