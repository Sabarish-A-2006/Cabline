package com.sclaria.cabbooking.api;

import com.sclaria.cabbooking.api.dto.AdminMetricsView;
import com.sclaria.cabbooking.api.dto.PromoCodeRequest;
import com.sclaria.cabbooking.api.dto.PromoCodeView;
import com.sclaria.cabbooking.api.dto.RideView;
import com.sclaria.cabbooking.api.dto.SupportTicketView;
import com.sclaria.cabbooking.api.dto.TicketStatusRequest;
import com.sclaria.cabbooking.api.dto.UserView;
import com.sclaria.cabbooking.service.AdminService;
import com.sclaria.cabbooking.service.PromoCodeService;
import com.sclaria.cabbooking.service.SupportTicketService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService admin;
    private final SupportTicketService tickets;
    private final PromoCodeService promos;

    public AdminController(AdminService admin, SupportTicketService tickets, PromoCodeService promos) {
        this.admin = admin;
        this.tickets = tickets;
        this.promos = promos;
    }

    @GetMapping("/metrics")
    public AdminMetricsView metrics() { return admin.metrics(); }

    @GetMapping("/users")
    public List<UserView> users() { return admin.users(); }

    @GetMapping("/rides")
    public List<RideView> rides() { return admin.rides(); }

    @GetMapping("/support/tickets")
    public List<SupportTicketView> tickets() { return tickets.all(); }

    @PatchMapping("/support/tickets/{ticketId}")
    public SupportTicketView updateTicket(@PathVariable UUID ticketId,
                                          @Valid @RequestBody TicketStatusRequest request) {
        return tickets.updateStatus(ticketId, request);
    }

    @GetMapping("/promos")
    public List<PromoCodeView> promos() { return promos.list(); }

    @PostMapping("/promos")
    @ResponseStatus(HttpStatus.CREATED)
    public PromoCodeView createPromo(@Valid @RequestBody PromoCodeRequest request) { return promos.create(request); }

    @PatchMapping("/promos/{promoId}/active")
    public PromoCodeView deactivatePromo(@PathVariable UUID promoId) { return promos.setActive(promoId, false); }
}
