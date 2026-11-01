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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rides")
public class Ride {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rider_id", nullable = false)
    private AppUser rider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private AppUser driver;

    @Column(nullable = false, length = 200)
    private String pickup;

    @Column(nullable = false, length = 200)
    private String dropoff;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedFare;

    @Column(precision = 10, scale = 2)
    private BigDecimal finalFare;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(length = 30)
    private String promoCode;

    private Long routeDurationSeconds;

    @Column(length = 2000)
    private String routePolyline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RideStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Ride() {}

    public Ride(AppUser rider, String pickup, String dropoff, BigDecimal distanceKm, BigDecimal estimatedFare) {
        this(rider, pickup, dropoff, distanceKm, estimatedFare, BigDecimal.ZERO, null, null, null);
    }

    public Ride(AppUser rider, String pickup, String dropoff, BigDecimal distanceKm, BigDecimal estimatedFare,
                BigDecimal discountAmount, String promoCode, Long routeDurationSeconds, String routePolyline) {
        this.rider = rider;
        this.pickup = pickup;
        this.dropoff = dropoff;
        this.distanceKm = distanceKm;
        this.estimatedFare = estimatedFare;
        this.discountAmount = discountAmount;
        this.promoCode = promoCode;
        this.routeDurationSeconds = routeDurationSeconds;
        this.routePolyline = routePolyline;
        this.status = RideStatus.REQUESTED;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void accept(AppUser driver) {
        requireStatus(RideStatus.REQUESTED, "Only a requested ride can be accepted");
        this.driver = driver;
        this.status = RideStatus.ACCEPTED;
        touch();
    }

    public void reject() {
        requireStatus(RideStatus.REQUESTED, "Only a requested ride can be rejected");
        this.status = RideStatus.REJECTED;
        touch();
    }

    public void start(AppUser driver) {
        requireAssignedDriver(driver);
        requireStatus(RideStatus.ACCEPTED, "Only an accepted ride can start");
        this.status = RideStatus.IN_PROGRESS;
        touch();
    }

    public void complete(AppUser driver) {
        requireAssignedDriver(driver);
        requireStatus(RideStatus.IN_PROGRESS, "Only an in-progress ride can complete");
        this.status = RideStatus.COMPLETED;
        this.finalFare = estimatedFare;
        touch();
    }

    public void cancel(AppUser rider) {
        if (!this.rider.getId().equals(rider.getId())) {
            throw new DomainConflictException("Only the rider can cancel this ride");
        }
        if (status != RideStatus.REQUESTED && status != RideStatus.ACCEPTED) {
            throw new DomainConflictException("This ride can no longer be cancelled");
        }
        this.status = RideStatus.CANCELLED;
        touch();
    }

    private void requireAssignedDriver(AppUser candidate) {
        boolean sameDriver = driver != null && (driver == candidate
                || (driver.getId() != null && driver.getId().equals(candidate.getId())));
        if (!sameDriver) {
            throw new DomainConflictException("This ride is assigned to another driver");
        }
    }

    private void requireStatus(RideStatus expected, String message) {
        if (status != expected) throw new DomainConflictException(message);
    }

    private void touch() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public AppUser getRider() { return rider; }
    public AppUser getDriver() { return driver; }
    public String getPickup() { return pickup; }
    public String getDropoff() { return dropoff; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public BigDecimal getEstimatedFare() { return estimatedFare; }
    public BigDecimal getFinalFare() { return finalFare; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public String getPromoCode() { return promoCode; }
    public Long getRouteDurationSeconds() { return routeDurationSeconds; }
    public String getRoutePolyline() { return routePolyline; }
    public RideStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
