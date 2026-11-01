package com.sclaria.cabbooking.service;

import com.sclaria.cabbooking.api.dto.AuthResponse;
import com.sclaria.cabbooking.api.dto.LoginRequest;
import com.sclaria.cabbooking.api.dto.RegisterRequest;
import com.sclaria.cabbooking.domain.AppUser;
import com.sclaria.cabbooking.domain.DriverProfile;
import com.sclaria.cabbooking.domain.UserRole;
import com.sclaria.cabbooking.error.DomainConflictException;
import com.sclaria.cabbooking.repository.AppUserRepository;
import com.sclaria.cabbooking.repository.DriverProfileRepository;
import com.sclaria.cabbooking.security.JwtTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final DriverProfileRepository drivers;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokens;

    public AuthService(AppUserRepository users, DriverProfileRepository drivers,
                       PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       JwtTokenService tokens) {
        this.users = users;
        this.drivers = drivers;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw new DomainConflictException("Admin accounts cannot be created through public registration");
        }
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) throw new DomainConflictException("An account with this email already exists");
        AppUser user = users.save(new AppUser(request.name().trim(), email,
                passwordEncoder.encode(request.password()), request.role()));
        if (request.role() == UserRole.DRIVER) drivers.save(new DriverProfile(user));
        return tokens.issueFor(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        AppUser user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new DomainConflictException("Invalid email or password"));
        return tokens.issueFor(user);
    }
}
