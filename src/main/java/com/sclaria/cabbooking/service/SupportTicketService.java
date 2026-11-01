package com.sclaria.cabbooking.service;

import com.sclaria.cabbooking.api.dto.SupportTicketRequest;
import com.sclaria.cabbooking.api.dto.SupportTicketView;
import com.sclaria.cabbooking.api.dto.TicketStatusRequest;
import com.sclaria.cabbooking.domain.SupportTicket;
import com.sclaria.cabbooking.error.DomainConflictException;
import com.sclaria.cabbooking.error.ResourceNotFoundException;
import com.sclaria.cabbooking.repository.RideRepository;
import com.sclaria.cabbooking.repository.SupportTicketRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportTicketService {
    private final CurrentUserService currentUser;
    private final RideRepository rides;
    private final SupportTicketRepository tickets;

    public SupportTicketService(CurrentUserService currentUser, RideRepository rides, SupportTicketRepository tickets) {
        this.currentUser = currentUser;
        this.rides = rides;
        this.tickets = tickets;
    }

    @Transactional
    public SupportTicketView create(Authentication authentication, SupportTicketRequest request) {
        var user = currentUser.resolve(authentication);
        var ride = request.rideId() == null ? null : rides.findById(request.rideId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));
        if (ride != null && !ride.getRider().getId().equals(user.getId())
                && (ride.getDriver() == null || !ride.getDriver().getId().equals(user.getId()))) {
            throw new DomainConflictException("You can only attach a ticket to your own ride");
        }
        return SupportTicketView.from(tickets.save(new SupportTicket(user, ride, request.subject().trim(), request.message().trim())));
    }

    @Transactional(readOnly = true)
    public List<SupportTicketView> mine(Authentication authentication) {
        var user = currentUser.resolve(authentication);
        return tickets.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(SupportTicketView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SupportTicketView> all() {
        return tickets.findAllByOrderByCreatedAtDesc().stream().map(SupportTicketView::from).toList();
    }

    @Transactional
    public SupportTicketView updateStatus(UUID ticketId, TicketStatusRequest request) {
        SupportTicket ticket = tickets.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));
        ticket.updateStatus(request.status());
        return SupportTicketView.from(ticket);
    }
}
