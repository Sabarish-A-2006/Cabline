package com.sclaria.cabbooking.service;

import com.sclaria.cabbooking.api.dto.AdminMetricsView;
import com.sclaria.cabbooking.api.dto.RideView;
import com.sclaria.cabbooking.api.dto.UserView;
import com.sclaria.cabbooking.config.FareProperties;
import com.sclaria.cabbooking.domain.PaymentStatus;
import com.sclaria.cabbooking.domain.RideStatus;
import com.sclaria.cabbooking.domain.TicketStatus;
import com.sclaria.cabbooking.domain.UserRole;
import com.sclaria.cabbooking.repository.AppUserRepository;
import com.sclaria.cabbooking.repository.DriverProfileRepository;
import com.sclaria.cabbooking.repository.PaymentRepository;
import com.sclaria.cabbooking.repository.RideRepository;
import com.sclaria.cabbooking.repository.SupportTicketRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final AppUserRepository users;
    private final DriverProfileRepository drivers;
    private final RideRepository rides;
    private final PaymentRepository payments;
    private final SupportTicketRepository tickets;
    private final FareProperties fareProperties;

    public AdminService(AppUserRepository users, DriverProfileRepository drivers, RideRepository rides,
                        PaymentRepository payments, SupportTicketRepository tickets, FareProperties fareProperties) {
        this.users = users;
        this.drivers = drivers;
        this.rides = rides;
        this.payments = payments;
        this.tickets = tickets;
        this.fareProperties = fareProperties;
    }

    @Transactional(readOnly = true)
    public AdminMetricsView metrics() {
        return new AdminMetricsView(users.count(), users.countByRole(UserRole.DRIVER), rides.count(),
                rides.countByStatusIn(List.of(RideStatus.REQUESTED, RideStatus.ACCEPTED, RideStatus.IN_PROGRESS)),
                payments.countByStatus(PaymentStatus.PENDING), payments.countByStatus(PaymentStatus.PAID),
                tickets.countByStatus(TicketStatus.OPEN));
    }

    @Transactional(readOnly = true)
    public List<UserView> users() { return users.findAll().stream().map(UserView::from).toList(); }

    @Transactional(readOnly = true)
    public List<RideView> rides() {
        return rides.findAll().stream().map(ride -> RideView.from(ride, fareProperties.getCurrency())).toList();
    }
}
