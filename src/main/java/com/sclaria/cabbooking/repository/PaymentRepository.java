package com.sclaria.cabbooking.repository;

import com.sclaria.cabbooking.domain.Payment;
import com.sclaria.cabbooking.domain.PaymentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByRideId(UUID rideId);
    List<Payment> findByRideRiderIdOrderByCreatedAtDesc(UUID riderId);
    long countByStatus(PaymentStatus status);
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.stripePaymentIntentId = :intentId")
    Optional<Payment> findByStripePaymentIntentIdForUpdate(@Param("intentId") String intentId);
}
