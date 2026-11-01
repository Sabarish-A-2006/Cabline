package com.sclaria.cabbooking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SupportTicketRequest(@NotBlank @Size(max = 140) String subject,
                                   @NotBlank @Size(min = 10, max = 4000) String message,
                                   UUID rideId) {}
