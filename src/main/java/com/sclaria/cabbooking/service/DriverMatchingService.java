package com.sclaria.cabbooking.service;

import com.sclaria.cabbooking.domain.DriverProfile;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DriverMatchingService {
    private final double radiusKm;

    public DriverMatchingService(@Value("${app.maps.matching-radius-km:15}") double radiusKm) {
        this.radiusKm = radiusKm;
    }

    public List<DriverProfile> match(List<DriverProfile> available, Double pickupLatitude, Double pickupLongitude) {
        if (pickupLatitude == null || pickupLongitude == null) return available;
        List<DriverProfile> located = available.stream()
                .filter(driver -> driver.getLatitude() != null && driver.getLongitude() != null)
                .filter(driver -> distanceKm(pickupLatitude, pickupLongitude, driver.getLatitude(), driver.getLongitude()) <= radiusKm)
                .sorted(Comparator.comparingDouble(driver -> distanceKm(pickupLatitude, pickupLongitude, driver.getLatitude(), driver.getLongitude())))
                .toList();
        return located.isEmpty() ? available : located;
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * earthRadiusKm * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
