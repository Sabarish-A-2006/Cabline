package com.sclaria.cabbooking.repository;

import com.sclaria.cabbooking.domain.SupportTicket;
import com.sclaria.cabbooking.domain.TicketStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {
    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
    long countByStatus(TicketStatus status);
}
