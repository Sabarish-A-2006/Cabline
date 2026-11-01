package com.sclaria.cabbooking.api;

import com.sclaria.cabbooking.api.dto.AvailabilityRequest;
import com.sclaria.cabbooking.api.dto.DriverAvailabilityView;
import com.sclaria.cabbooking.service.DriverService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/drivers/me")
public class DriverController {
    private final DriverService drivers;

    public DriverController(DriverService drivers) { this.drivers = drivers; }

    @PatchMapping("/availability")
    @PreAuthorize("hasRole('DRIVER')")
    public Map<String, Boolean> setAvailability(Authentication authentication,
                                               @Valid @RequestBody AvailabilityRequest request) {
        return Map.of("available", drivers.setAvailability(authentication, request));
    }

    @GetMapping("/availability")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverAvailabilityView getAvailability(Authentication authentication) {
        return drivers.getAvailability(authentication);
    }
}
