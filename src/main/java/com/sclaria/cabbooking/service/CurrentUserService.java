package com.sclaria.cabbooking.service;

import com.sclaria.cabbooking.domain.AppUser;
import com.sclaria.cabbooking.domain.UserRole;
import com.sclaria.cabbooking.error.ResourceNotFoundException;
import com.sclaria.cabbooking.repository.AppUserRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final AppUserRepository users;

    public CurrentUserService(AppUserRepository users) { this.users = users; }

    public AppUser require(Authentication authentication, UserRole role) {
        AppUser user = resolve(authentication);
        if (user.getRole() != role) throw new com.sclaria.cabbooking.error.DomainConflictException("This operation requires a " + role + " account");
        return user;
    }

    public AppUser resolve(Authentication authentication) {
        UUID userId;
        try {
            userId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResourceNotFoundException("Authenticated account was not found");
        }
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated account was not found"));
        return user;
    }

    public AppUser requireById(UUID id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }
}
