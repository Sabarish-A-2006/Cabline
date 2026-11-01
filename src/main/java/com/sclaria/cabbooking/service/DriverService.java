package com.sclaria.cabbooking.service;

import com.sclaria.cabbooking.api.dto.AvailabilityRequest;
import com.sclaria.cabbooking.api.dto.DriverAvailabilityView;
import com.sclaria.cabbooking.domain.UserRole;
import com.sclaria.cabbooking.error.ResourceNotFoundException;
import com.sclaria.cabbooking.repository.DriverProfileRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverService {
    private final CurrentUserService currentUser;
    private final DriverProfileRepository drivers;

    public DriverService(CurrentUserService currentUser, DriverProfileRepository drivers) {
        this.currentUser = currentUser;
        this.drivers = drivers;
    }

    @Transactional
    public boolean setAvailability(Authentication authentication, AvailabilityRequest request) {
        var user = currentUser.require(authentication, UserRole.DRIVER);
        var profile = drivers.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        profile.setAvailable(request.available());
        if (request.latitude() != null && request.longitude() != null) {
            profile.updateLocation(request.latitude(), request.longitude());
        }
        return profile.isAvailable();
    }

    @Transactional(readOnly = true)
    public DriverAvailabilityView getAvailability(Authentication authentication) {
        var user = currentUser.require(authentication, UserRole.DRIVER);
        var profile = drivers.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        return new DriverAvailabilityView(profile.isAvailable(), profile.getLatitude(), profile.getLongitude());
    }
}
