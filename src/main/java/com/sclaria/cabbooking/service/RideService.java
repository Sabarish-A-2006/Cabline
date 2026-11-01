package com.sclaria.cabbooking.service;

import com.sclaria.cabbooking.api.dto.BookingRequest;
import com.sclaria.cabbooking.api.dto.OfferView;
import com.sclaria.cabbooking.api.dto.RideView;
import com.sclaria.cabbooking.config.FareProperties;
import com.sclaria.cabbooking.domain.AppUser;
import com.sclaria.cabbooking.domain.DriverProfile;
import com.sclaria.cabbooking.domain.OfferStatus;
import com.sclaria.cabbooking.domain.Ride;
import com.sclaria.cabbooking.domain.RideOffer;
import com.sclaria.cabbooking.domain.UserRole;
import com.sclaria.cabbooking.error.DomainConflictException;
import com.sclaria.cabbooking.error.ResourceNotFoundException;
import com.sclaria.cabbooking.repository.DriverProfileRepository;
import com.sclaria.cabbooking.repository.PaymentRepository;
import com.sclaria.cabbooking.repository.RideOfferRepository;
import com.sclaria.cabbooking.repository.RideRepository;
import com.sclaria.cabbooking.repository.PromoCodeRepository;
import com.sclaria.cabbooking.domain.PromoCode;
import com.sclaria.cabbooking.api.dto.RouteEstimateView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideService {
    private final CurrentUserService currentUser;
    private final FareCalculator fareCalculator;
    private final FareProperties fareProperties;
    private final DriverProfileRepository drivers;
    private final RideRepository rides;
    private final RideOfferRepository offers;
    private final PaymentRepository payments;
    private final RideEventPublisher events;
    private final RouteEstimateService routeEstimates;
    private final DriverMatchingService driverMatching;
    private final PromoCodeRepository promoCodes;

    public RideService(CurrentUserService currentUser, FareCalculator fareCalculator,
                       FareProperties fareProperties, DriverProfileRepository drivers,
                       RideRepository rides, RideOfferRepository offers, PaymentRepository payments,
                       RideEventPublisher events, RouteEstimateService routeEstimates,
                       DriverMatchingService driverMatching, PromoCodeRepository promoCodes) {
        this.currentUser = currentUser;
        this.fareCalculator = fareCalculator;
        this.fareProperties = fareProperties;
        this.drivers = drivers;
        this.rides = rides;
        this.offers = offers;
        this.payments = payments;
        this.events = events;
        this.routeEstimates = routeEstimates;
        this.driverMatching = driverMatching;
        this.promoCodes = promoCodes;
    }

    @Transactional
    public RideView book(Authentication authentication, BookingRequest request) {
        AppUser rider = currentUser.require(authentication, UserRole.RIDER);
        RouteEstimateView route = routeEstimates.estimate(request.pickup().trim(), request.dropoff().trim(), request.distanceKm());
        PromoCode promo = null;
        if (request.promoCode() != null && !request.promoCode().isBlank()) {
            promo = promoCodes.findLockedByCode(request.promoCode().trim().toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> new com.sclaria.cabbooking.error.ResourceNotFoundException("Promo code not found"));
            promo.requireUsable(Instant.now());
            promo.recordUse();
        }
        BigDecimal grossFare = fareCalculator.estimate(route.distanceKm());
        BigDecimal discountPercent = promo == null ? BigDecimal.ZERO : promo.getDiscountPercent();
        BigDecimal discount = fareCalculator.discountAmount(grossFare, discountPercent);
        BigDecimal estimatedFare = grossFare.subtract(discount).max(BigDecimal.ZERO);
        Ride ride = rides.save(new Ride(rider, request.pickup().trim(), request.dropoff().trim(),
                route.distanceKm(), estimatedFare, discount, promo == null ? null : promo.getCode(),
                route.durationSeconds(), route.polyline()));
        List<RideOffer> newOffers = driverMatching.match(drivers.findByAvailableTrue(), request.pickupLatitude(), request.pickupLongitude()).stream()
                .map(DriverProfile::getUser)
                .map(driver -> new RideOffer(ride, driver))
                .toList();
        if (!newOffers.isEmpty()) offers.saveAll(newOffers);
        events.publish(ride);
        return RideView.from(ride, fareProperties.getCurrency());
    }

    @Transactional(readOnly = true)
    public List<RideView> myRides(Authentication authentication) {
        var user = currentUser.require(authentication, resolveRole(authentication));
        List<Ride> results = user.getRole() == UserRole.RIDER
                ? rides.findByRiderIdOrderByCreatedAtDesc(user.getId())
                : rides.findByDriverIdOrderByCreatedAtDesc(user.getId());
        return results.stream().map(ride -> RideView.from(ride, fareProperties.getCurrency())).toList();
    }

    @Transactional
    public RideView cancel(Authentication authentication, UUID rideId) {
        AppUser rider = currentUser.require(authentication, UserRole.RIDER);
        Ride ride = rides.findByIdForUpdate(rideId).orElseThrow(() -> new ResourceNotFoundException("Ride not found"));
        ride.cancel(rider);
        if (ride.getDriver() != null) {
            drivers.findByUserId(ride.getDriver().getId()).ifPresent(profile -> profile.setAvailable(true));
        }
        offers.findByRideIdAndStatus(rideId, OfferStatus.PENDING).forEach(RideOffer::withdraw);
        events.publish(ride);
        return RideView.from(ride, fareProperties.getCurrency());
    }

    @Transactional(readOnly = true)
    public List<OfferView> myPendingOffers(Authentication authentication) {
        AppUser driver = currentUser.require(authentication, UserRole.DRIVER);
        return offers.findByDriverIdAndStatusOrderByCreatedAtDesc(driver.getId(), OfferStatus.PENDING)
                .stream().map(offer -> OfferView.from(offer, fareProperties.getCurrency())).toList();
    }

    @Transactional
    public RideView respondToOffer(Authentication authentication, UUID offerId, boolean accept) {
        AppUser driver = currentUser.require(authentication, UserRole.DRIVER);
        DriverProfile profile = drivers.findByUserId(driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        RideOffer offer = offers.findByIdAndDriverId(offerId, driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride offer not found"));
        Ride ride = rides.findByIdForUpdate(offer.getRide().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));
        if (ride.getStatus() != com.sclaria.cabbooking.domain.RideStatus.REQUESTED) {
            throw new DomainConflictException("This ride is no longer available");
        }
        if (accept) {
            if (!profile.isAvailable()) throw new DomainConflictException("Set your driver status to available before accepting a ride");
            offer.accept();
            ride.accept(driver);
            profile.setAvailable(false);
            offers.findByRideIdAndStatus(ride.getId(), OfferStatus.PENDING).forEach(RideOffer::withdraw);
        } else {
            offer.decline();
            if (offers.countByRideIdAndStatus(ride.getId(), OfferStatus.PENDING) == 0) ride.reject();
        }
        events.publish(ride);
        return RideView.from(ride, fareProperties.getCurrency());
    }

    @Transactional
    public RideView start(Authentication authentication, UUID rideId) {
        AppUser driver = currentUser.require(authentication, UserRole.DRIVER);
        Ride ride = rides.findByIdForUpdate(rideId).orElseThrow(() -> new ResourceNotFoundException("Ride not found"));
        ride.start(driver);
        events.publish(ride);
        return RideView.from(ride, fareProperties.getCurrency());
    }

    @Transactional
    public RideView complete(Authentication authentication, UUID rideId) {
        AppUser driver = currentUser.require(authentication, UserRole.DRIVER);
        Ride ride = rides.findByIdForUpdate(rideId).orElseThrow(() -> new ResourceNotFoundException("Ride not found"));
        ride.complete(driver);
        payments.findByRideId(rideId).orElseGet(() -> payments.save(
                new com.sclaria.cabbooking.domain.Payment(ride, ride.getFinalFare(), fareProperties.getCurrency())));
        drivers.findByUserId(driver.getId()).ifPresent(profile -> profile.setAvailable(true));
        events.publish(ride);
        return RideView.from(ride, fareProperties.getCurrency());
    }

    private UserRole resolveRole(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_RIDER"))
                ? UserRole.RIDER : UserRole.DRIVER;
    }
}
