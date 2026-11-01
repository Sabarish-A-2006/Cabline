package com.sclaria.cabbooking.config;

import com.sclaria.cabbooking.domain.AppUser;
import com.sclaria.cabbooking.domain.UserRole;
import com.sclaria.cabbooking.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Configuration
public class AdminBootstrap {
    @Bean
    ApplicationRunner createConfiguredAdmin(AppUserRepository users, PasswordEncoder passwordEncoder,
                                            @Value("${app.admin.email:}") String email,
                                            @Value("${app.admin.password:}") String password) {
        return arguments -> {
            if (!StringUtils.hasText(email) && !StringUtils.hasText(password)) return;
            if (!StringUtils.hasText(email) || !StringUtils.hasText(password) || password.length() < 10) {
                throw new IllegalStateException("Set both APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD (at least 10 characters)");
            }
            String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
            if (!users.existsByEmailIgnoreCase(normalizedEmail)) {
                users.save(new AppUser("Administrator", normalizedEmail, passwordEncoder.encode(password), UserRole.ADMIN));
            }
        };
    }
}
