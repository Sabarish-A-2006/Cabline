package com.sclaria.cabbooking.service;

import com.sclaria.cabbooking.domain.Payment;
import com.sclaria.cabbooking.domain.PaymentStatus;
import com.sclaria.cabbooking.domain.UserRole;
import com.sclaria.cabbooking.error.DomainConflictException;
import com.sclaria.cabbooking.error.ResourceNotFoundException;
import com.sclaria.cabbooking.repository.PaymentRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceiptService {
    private final CurrentUserService currentUser;
    private final PaymentRepository payments;

    public ReceiptService(CurrentUserService currentUser, PaymentRepository payments) {
        this.currentUser = currentUser;
        this.payments = payments;
    }

    @Transactional(readOnly = true)
    public byte[] create(Authentication authentication, UUID paymentId) {
        var rider = currentUser.require(authentication, UserRole.RIDER);
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!payment.getRide().getRider().getId().equals(rider.getId())) {
            throw new DomainConflictException("This payment belongs to another rider");
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new DomainConflictException("A receipt is available after payment is complete");
        }
        return createPdfForPaidPayment(payment);
    }

    public byte[] createPdfForPaidPayment(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new DomainConflictException("A receipt is available after payment is complete");
        }
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                content.beginText();
                content.setFont(bold, 22);
                content.newLineAtOffset(55, 740);
                content.showText("Cab Booking - Payment Receipt");
                content.setFont(regular, 12);
                content.newLineAtOffset(0, -42);
                content.showText("Receipt: " + payment.getId());
                content.newLineAtOffset(0, -22);
                content.showText("Ride: " + payment.getRide().getId());
                content.newLineAtOffset(0, -22);
                content.showText("Rider: " + safe(payment.getRide().getRider().getName()) + " (" + safe(payment.getRide().getRider().getEmail()) + ")");
                content.newLineAtOffset(0, -22);
                content.showText("Route: " + safe(payment.getRide().getPickup()) + " to " + safe(payment.getRide().getDropoff()));
                content.newLineAtOffset(0, -22);
                content.showText("Distance: " + payment.getRide().getDistanceKm() + " km");
                content.newLineAtOffset(0, -22);
                content.showText("Paid: " + payment.getCurrency() + " " + payment.getAmount());
                content.newLineAtOffset(0, -22);
                content.showText("Status: " + payment.getStatus() + " | " + payment.getCreatedAt());
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the payment receipt", exception);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
