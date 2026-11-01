package com.sclaria.cabbooking.api.dto;

import com.sclaria.cabbooking.domain.SupportTicket;
import com.sclaria.cabbooking.domain.TicketStatus;
import java.time.Instant;
import java.util.UUID;

public record SupportTicketView(UUID id, UUID userId, String userName, UUID rideId,
                                String subject, String message, TicketStatus status,
                                Instant createdAt, Instant updatedAt) {
    public static SupportTicketView from(SupportTicket ticket) {
        return new SupportTicketView(ticket.getId(), ticket.getUser().getId(), ticket.getUser().getName(),
                ticket.getRide() == null ? null : ticket.getRide().getId(), ticket.getSubject(),
                ticket.getMessage(), ticket.getStatus(), ticket.getCreatedAt(), ticket.getUpdatedAt());
    }
}
