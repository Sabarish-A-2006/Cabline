package com.sclaria.cabbooking.security;

import com.sclaria.cabbooking.api.dto.AuthResponse;
import com.sclaria.cabbooking.api.dto.UserView;
import com.sclaria.cabbooking.domain.AppUser;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final JwtEncoder encoder;
    private final Duration lifetime;

    public JwtTokenService(JwtEncoder encoder, @Value("${app.jwt.lifetime:PT1H}") Duration lifetime) {
        this.encoder = encoder;
        this.lifetime = lifetime;
    }

    public AuthResponse issueFor(AppUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(lifetime);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cab-booking-api")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", expiresAt, UserView.from(user));
    }
}
