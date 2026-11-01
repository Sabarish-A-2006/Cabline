package com.sclaria.cabbooking.repository;

import com.sclaria.cabbooking.domain.PromoCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID> {
    Optional<PromoCode> findByCodeIgnoreCase(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PromoCode p where upper(p.code) = upper(:code)")
    Optional<PromoCode> findLockedByCode(@Param("code") String code);
    boolean existsByCodeIgnoreCase(String code);
}
