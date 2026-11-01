package com.sclaria.cabbooking.api.dto;

import com.sclaria.cabbooking.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record TicketStatusRequest(@NotNull TicketStatus status) {}
