package com.sclaria.cabbooking.service;

import com.sclaria.cabbooking.domain.Ride;
import com.sclaria.cabbooking.domain.RideStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RideEventPublisher {
    private final SimpMessagingTemplate messaging;

    public RideEventPublisher(SimpMessagingTemplate messaging) { this.messaging = messaging; }

    public void publish(Ride ride) {
        messaging.convertAndSend("/topic/rides/" + ride.getId(),
                new RideEvent(ride.getId(), ride.getStatus(), ride.getUpdatedAt()));
    }

    public record RideEvent(UUID rideId, RideStatus status, Instant updatedAt) {}
}
