package com.sclaria.cabbooking.service;

import com.sclaria.cabbooking.api.dto.PromoCodeRequest;
import com.sclaria.cabbooking.api.dto.PromoCodeView;
import com.sclaria.cabbooking.domain.PromoCode;
import com.sclaria.cabbooking.error.DomainConflictException;
import com.sclaria.cabbooking.error.ResourceNotFoundException;
import com.sclaria.cabbooking.repository.PromoCodeRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromoCodeService {
    private final PromoCodeRepository promos;

    public PromoCodeService(PromoCodeRepository promos) { this.promos = promos; }

    @Transactional(readOnly = true)
    public List<PromoCodeView> list() {
        return promos.findAll().stream().map(PromoCodeView::from).toList();
    }

    @Transactional
    public PromoCodeView create(PromoCodeRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (promos.existsByCodeIgnoreCase(code)) throw new DomainConflictException("Promo code already exists");
        return PromoCodeView.from(promos.save(new PromoCode(code, request.discountPercent(), request.expiresAt(), request.maxUses())));
    }

    @Transactional
    public PromoCodeView setActive(UUID id, boolean active) {
        PromoCode promo = promos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));
        promo.setActive(active);
        return PromoCodeView.from(promo);
    }
}
