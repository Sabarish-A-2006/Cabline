package com.sclaria.cabbooking.api;

import com.sclaria.cabbooking.api.dto.SupportTicketRequest;
import com.sclaria.cabbooking.api.dto.SupportTicketView;
import com.sclaria.cabbooking.service.SupportTicketService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support/tickets")
public class SupportController {
    private final SupportTicketService tickets;

    public SupportController(SupportTicketService tickets) { this.tickets = tickets; }

    @GetMapping("/me")
    public List<SupportTicketView> mine(Authentication authentication) { return tickets.mine(authentication); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupportTicketView create(Authentication authentication, @Valid @RequestBody SupportTicketRequest request) {
        return tickets.create(authentication, request);
    }
}
