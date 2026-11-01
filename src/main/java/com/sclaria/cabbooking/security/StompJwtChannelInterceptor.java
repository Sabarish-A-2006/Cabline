package com.sclaria.cabbooking.security;

import com.sclaria.cabbooking.repository.RideRepository;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {
    private final JwtDecoder jwtDecoder;
    private final RideRepository rides;

    public StompJwtChannelInterceptor(JwtDecoder jwtDecoder, RideRepository rides) {
        this.jwtDecoder = jwtDecoder;
        this.rides = rides;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeRideSubscription(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessageDeliveryException("A bearer token is required to connect");
        }
        var jwt = jwtDecoder.decode(header.substring("Bearer ".length()).trim());
        String role = jwt.getClaimAsString("role");
        List<SimpleGrantedAuthority> authorities = role == null
                ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
        accessor.setUser(new UsernamePasswordAuthenticationToken(jwt.getSubject(), null, authorities));
    }

    private void authorizeRideSubscription(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        String destination = accessor.getDestination();
        if (principal == null || destination == null || !destination.matches("/topic/rides/[0-9a-fA-F-]{36}")) {
            throw new MessageDeliveryException("Only authenticated ride subscriptions are allowed");
        }
        UUID rideId;
        try {
            rideId = UUID.fromString(destination.substring("/topic/rides/".length()));
        } catch (IllegalArgumentException exception) {
            throw new MessageDeliveryException("Invalid ride subscription");
        }
        boolean isParticipant = rides.findById(rideId).map(ride ->
                ride.getRider().getId().toString().equals(principal.getName())
                        || (ride.getDriver() != null && ride.getDriver().getId().toString().equals(principal.getName())))
                .orElse(false);
        if (!isParticipant) throw new MessageDeliveryException("You are not a participant in this ride");
    }
}
