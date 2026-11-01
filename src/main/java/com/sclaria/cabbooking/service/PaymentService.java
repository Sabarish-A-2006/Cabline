package com.sclaria.cabbooking.service;

import com.sclaria.cabbooking.api.dto.PaymentView;
import com.sclaria.cabbooking.api.dto.CheckoutView;
import com.sclaria.cabbooking.domain.AppUser;
import com.sclaria.cabbooking.domain.Payment;
import com.sclaria.cabbooking.domain.UserRole;
import com.sclaria.cabbooking.error.DomainConflictException;
import com.sclaria.cabbooking.error.ResourceNotFoundException;
import com.sclaria.cabbooking.repository.PaymentRepository;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.exception.SignatureVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final CurrentUserService currentUser;
    private final PaymentRepository payments;
    private final ReceiptService receipts;
    private final StripeClient stripeClient;
    private final String publishableKey;
    private final String webhookSecret;
    private final boolean mailEnabled;
    private final String mailFrom;
    private final JavaMailSender mailSender;

    public PaymentService(CurrentUserService currentUser, PaymentRepository payments, ReceiptService receipts,
                          @Value("${app.stripe.secret-key:}") String stripeSecret,
                          @Value("${app.stripe.publishable-key:}") String publishableKey,
                          @Value("${app.stripe.webhook-secret:}") String webhookSecret,
                          @Value("${app.mail.enabled:false}") boolean mailEnabled,
                          @Value("${app.mail.from:no-reply@example.test}") String mailFrom,
                          ObjectProvider<JavaMailSender> mailSenders) {
        this.currentUser = currentUser;
        this.payments = payments;
        this.receipts = receipts;
        this.stripeClient = stripeSecret == null || stripeSecret.isBlank() ? null : new StripeClient(stripeSecret.trim());
        this.publishableKey = publishableKey;
        this.webhookSecret = webhookSecret;
        this.mailEnabled = mailEnabled;
        this.mailFrom = mailFrom;
        this.mailSender = mailSenders.getIfAvailable();
    }

    @Transactional(readOnly = true)
    public List<PaymentView> myPayments(Authentication authentication) {
        AppUser rider = currentUser.require(authentication, UserRole.RIDER);
        return payments.findByRideRiderIdOrderByCreatedAtDesc(rider.getId()).stream()
                .map(PaymentView::from).toList();
    }

    @Transactional
    public PaymentView confirmDemoPayment(Authentication authentication, UUID paymentId) {
        Payment payment = requireOwnedPayment(authentication, paymentId, true);
        if (stripeClient != null || payment.getStripePaymentIntentId() != null) {
            throw new DomainConflictException("Stripe payments are confirmed by the payment provider");
        }
        if (payment.getStatus() == com.sclaria.cabbooking.domain.PaymentStatus.PAID) return PaymentView.from(payment);
        payment.markPaid();
        notifyReceipt(payment);
        return PaymentView.from(payment);
    }

    @Transactional
    public CheckoutView createCheckout(Authentication authentication, UUID paymentId) {
        Payment payment = requireOwnedPayment(authentication, paymentId, true);
        if (payment.getStatus() == com.sclaria.cabbooking.domain.PaymentStatus.PAID) {
            return new CheckoutView(paymentId, "PAID", payment.getAmount(), payment.getCurrency(), null, null);
        }
        if (stripeClient == null) {
            return new CheckoutView(paymentId, "DEMO", payment.getAmount(), payment.getCurrency(), null, null);
        }
        if (payment.getStripeClientSecret() != null) {
            return new CheckoutView(paymentId, "STRIPE", payment.getAmount(), payment.getCurrency(),
                    payment.getStripeClientSecret(), publishableKey);
        }
        if (publishableKey == null || publishableKey.isBlank()) {
            throw new DomainConflictException("Configure STRIPE_PUBLISHABLE_KEY to enable card checkout");
        }
        try {
            long amountMinor = payment.getAmount().multiply(new BigDecimal("100"))
                    .setScale(0, RoundingMode.HALF_UP).longValueExact();
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountMinor)
                    .setCurrency(payment.getCurrency().toLowerCase(Locale.ROOT))
                    .putMetadata("paymentId", payment.getId().toString())
                    .putMetadata("rideId", payment.getRide().getId().toString())
                    .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true).build())
                    .build();
            PaymentIntent intent = stripeClient.v1().paymentIntents().create(params);
            payment.setStripeIntent(intent.getId(), intent.getClientSecret());
            return new CheckoutView(paymentId, "STRIPE", payment.getAmount(), payment.getCurrency(),
                    intent.getClientSecret(), publishableKey);
        } catch (StripeException | ArithmeticException exception) {
            log.error("Unable to start Stripe checkout for payment {}", paymentId, exception);
            throw new DomainConflictException("Card checkout is temporarily unavailable");
        }
    }

    @Transactional
    public void handleStripeWebhook(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new DomainConflictException("Stripe webhooks are not configured");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException | RuntimeException exception) {
            throw new DomainConflictException("Invalid Stripe webhook signature or payload");
        }
        if (!"payment_intent.succeeded".equals(event.getType())) return;
        Optional<com.stripe.model.StripeObject> stripeObject = event.getDataObjectDeserializer().getObject();
        if (stripeObject.isEmpty() || !(stripeObject.get() instanceof PaymentIntent intent)) return;
        payments.findByStripePaymentIntentIdForUpdate(intent.getId()).ifPresent(payment -> {
            if (payment.getStatus() != com.sclaria.cabbooking.domain.PaymentStatus.PAID) {
                payment.markPaid();
                notifyReceipt(payment);
            }
        });
    }

    private Payment requireOwnedPayment(Authentication authentication, UUID paymentId, boolean lock) {
        AppUser rider = currentUser.require(authentication, UserRole.RIDER);
        Payment payment = (lock ? payments.findByIdForUpdate(paymentId) : payments.findById(paymentId))
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!payment.getRide().getRider().getId().equals(rider.getId())) {
            throw new DomainConflictException("This payment belongs to another rider");
        }
        return payment;
    }

    private void notifyReceipt(Payment payment) {
        if (!mailEnabled || mailSender == null) return;
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(payment.getRide().getRider().getEmail());
            helper.setSubject("Cab booking payment receipt");
            helper.setText("Your payment of " + payment.getCurrency() + " " + payment.getAmount()
                    + " for ride " + payment.getRide().getId() + " is complete. Your PDF receipt is attached.");
            helper.addAttachment("cabline-receipt-" + payment.getId() + ".pdf",
                    new ByteArrayResource(receipts.createPdfForPaidPayment(payment)), "application/pdf");
            mailSender.send(message);
        } catch (Exception exception) {
            log.warn("Payment completed, but receipt email could not be delivered for payment {}", payment.getId(), exception);
        }
    }
}
