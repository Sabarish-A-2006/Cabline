package com.sclaria.cabbooking.security;

import com.sclaria.cabbooking.api.dto.AuthResponse;
import com.sclaria.cabbooking.domain.AppUser;
import com.sclaria.cabbooking.domain.UserRole;
import com.sclaria.cabbooking.repository.AppUserRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;
    private final String frontendUrl;

    public OAuthLoginSuccessHandler(AppUserRepository users, PasswordEncoder passwordEncoder,
                                    JwtTokenService tokens,
                                    @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User principal = ((OAuth2AuthenticationToken) authentication).getPrincipal();
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        if (email == null || email.isBlank()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Google account did not provide an email address");
            return;
        }
        if (Boolean.FALSE.equals(principal.getAttribute("email_verified"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Google account email must be verified");
            return;
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        AppUser user = users.findByEmailIgnoreCase(normalizedEmail).orElseGet(() -> users.save(new AppUser(
                name == null || name.isBlank() ? normalizedEmail : name.trim(), normalizedEmail,
                passwordEncoder.encode(UUID.randomUUID().toString()), UserRole.RIDER)));
        if (user.getRole() != UserRole.RIDER) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Google sign-in is only enabled for rider accounts");
            return;
        }
        AuthResponse auth = tokens.issueFor(user);
        String token = UriUtils.encode(auth.accessToken(), StandardCharsets.UTF_8);
        response.sendRedirect(frontendUrl + "/#accessToken=" + token);
    }
}
