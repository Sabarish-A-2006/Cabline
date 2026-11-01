package com.sclaria.cabbooking.api.dto;

import com.sclaria.cabbooking.domain.AppUser;
import com.sclaria.cabbooking.domain.UserRole;
import java.util.UUID;

public record UserView(UUID id, String name, String email, UserRole role) {
    public static UserView from(AppUser user) {
        return new UserView(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
