package com.sclaria.cabbooking.api;

import com.sclaria.cabbooking.api.dto.AuthResponse;
import com.sclaria.cabbooking.api.dto.LoginRequest;
import com.sclaria.cabbooking.api.dto.RegisterRequest;
import com.sclaria.cabbooking.api.dto.UserView;
import com.sclaria.cabbooking.service.CurrentUserService;
import com.sclaria.cabbooking.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final CurrentUserService currentUser;

    public AuthController(AuthService authService, CurrentUserService currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    public UserView me(Authentication authentication) { return UserView.from(currentUser.resolve(authentication)); }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
