package com.sclaria.cabbooking.api;

import com.sclaria.cabbooking.api.dto.PaymentView;
import com.sclaria.cabbooking.api.dto.CheckoutView;
import com.sclaria.cabbooking.service.PaymentService;
import com.sclaria.cabbooking.service.ReceiptService;
import java.util.UUID;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasRole('RIDER')")
public class PaymentController {
    private final PaymentService payments;
    private final ReceiptService receipts;

    public PaymentController(PaymentService payments, ReceiptService receipts) {
        this.payments = payments;
        this.receipts = receipts;
    }

    @GetMapping("/me")
    public List<PaymentView> myPayments(Authentication authentication) { return payments.myPayments(authentication); }

    @PostMapping("/{paymentId}/confirm-demo")
    public PaymentView confirmDemo(Authentication authentication, @PathVariable UUID paymentId) {
        return payments.confirmDemoPayment(authentication, paymentId);
    }

    @PostMapping("/{paymentId}/checkout")
    public CheckoutView checkout(Authentication authentication, @PathVariable UUID paymentId) {
        return payments.createCheckout(authentication, paymentId);
    }

    @GetMapping(value = "/{paymentId}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> receipt(Authentication authentication, @PathVariable UUID paymentId) {
        byte[] pdf = receipts.create(authentication, paymentId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt-" + paymentId + ".pdf")
                .body(pdf);
    }
}
