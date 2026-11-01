package com.sclaria.cabbooking.api.dto;

import java.time.Instant;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UserView user) {}
