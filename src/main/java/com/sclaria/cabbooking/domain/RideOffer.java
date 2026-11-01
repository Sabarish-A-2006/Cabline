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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ride_offers", uniqueConstraints = @UniqueConstraint(columnNames = {"ride_id", "driver_id"}))
public class RideOffer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private AppUser driver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status = OfferStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected RideOffer() {}

    public RideOffer(Ride ride, AppUser driver) {
        this.ride = ride;
        this.driver = driver;
    }

    public void accept() {
        requirePending();
        status = OfferStatus.ACCEPTED;
    }

    public void decline() {
        requirePending();
        status = OfferStatus.DECLINED;
    }

    public void withdraw() {
        if (status == OfferStatus.PENDING) status = OfferStatus.WITHDRAWN;
    }

    private void requirePending() {
        if (status != OfferStatus.PENDING) throw new DomainConflictException("This ride offer is no longer pending");
    }

    public UUID getId() { return id; }
    public Ride getRide() { return ride; }
    public AppUser getDriver() { return driver; }
    public OfferStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
