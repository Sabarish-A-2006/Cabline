package com.sclaria.cabbooking.api;

import com.sclaria.cabbooking.service.PaymentService;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {
    private final PaymentService payments;

    public StripeWebhookController(PaymentService payments) { this.payments = payments; }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> receive(@RequestBody String payload,
                                       @RequestHeader(name = "Stripe-Signature", required = false) String signature) {
        payments.handleStripeWebhook(payload, signature == null ? "" : signature);
        return Map.of("received", "true");
    }
}
