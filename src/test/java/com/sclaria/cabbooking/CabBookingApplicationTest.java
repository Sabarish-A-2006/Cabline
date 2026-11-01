package com.sclaria.cabbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.util.Map;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;
import com.sclaria.cabbooking.domain.PromoCode;
import com.sclaria.cabbooking.domain.AppUser;
import com.sclaria.cabbooking.domain.UserRole;
import com.sclaria.cabbooking.repository.AppUserRepository;
import com.sclaria.cabbooking.repository.PromoCodeRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class CabBookingApplicationTest {
    @Autowired
    private CabBookingApplication application;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PromoCodeRepository promoCodes;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void applicationContextStarts() {
        assertThat(application).isNotNull();
    }

    @Test
    void allowsRegistrationPreflightFromLocalNetworkFrontend() throws Exception {
        String origin = "http://192.168.1.7:5173";
        mockMvc.perform(options("/api/auth/register")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin));

        String email = "cors-" + UUID.randomUUID() + "@example.test";
        String password = "correct-horse-battery-staple";
        mockMvc.perform(post("/api/auth/register")
                        .header("Origin", origin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "CORS Check", "email", email,
                                "password", password, "role", "RIDER"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Access-Control-Allow-Origin", origin));
        mockMvc.perform(post("/api/auth/login")
                        .header("Origin", origin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin));
    }

    @Test
    void riderAndDriverCanCompleteTheDemoRideFlow() throws Exception {
        register("rider@example.test", "RIDER");
        String riderToken = login("rider@example.test");
        String driverToken = register("driver@example.test", "DRIVER");

        mockMvc.perform(patch("/api/drivers/me/availability")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":true}"))
                .andExpect(status().isOk());

        MvcResult booking = mockMvc.perform(post("/api/rides")
                        .header("Authorization", bearer(riderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickup\":\"Central Station\",\"dropoff\":\"Airport\",\"distanceKm\":10.0}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode ride = objectMapper.readTree(booking.getResponse().getContentAsString());
        String rideId = ride.get("id").asText();
        assertThat(ride.get("estimatedFare").decimalValue()).isEqualByComparingTo("230.00");

        MvcResult offers = mockMvc.perform(get("/api/drivers/me/offers")
                        .header("Authorization", bearer(driverToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode offerList = objectMapper.readTree(offers.getResponse().getContentAsString());
        String offerId = offerList.get(0).get("offerId").asText();

        mockMvc.perform(post("/api/driver-offers/{offerId}/accept", offerId)
                        .header("Authorization", bearer(driverToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/rides/{rideId}/start", rideId)
                        .header("Authorization", bearer(driverToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/rides/{rideId}/complete", rideId)
                        .header("Authorization", bearer(driverToken)))
                .andExpect(status().isOk());

        MvcResult payments = mockMvc.perform(get("/api/payments/me")
                        .header("Authorization", bearer(riderToken)))
                .andExpect(status().isOk())
                .andReturn();
        String paymentId = objectMapper.readTree(payments.getResponse().getContentAsString()).get(0).get("id").asText();

        MvcResult checkout = mockMvc.perform(post("/api/payments/{paymentId}/checkout", paymentId)
                        .header("Authorization", bearer(riderToken)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(checkout.getResponse().getContentAsString()).get("mode").asText()).isEqualTo("DEMO");

        mockMvc.perform(post("/api/payments/{paymentId}/confirm-demo", paymentId)
                        .header("Authorization", bearer(riderToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/payments/{paymentId}/receipt", paymentId)
                        .header("Authorization", bearer(riderToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        mockMvc.perform(post("/api/rides/{rideId}/rating", rideId)
                        .header("Authorization", bearer(riderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stars\":5,\"comment\":\"Great ride\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsInvalidBookingInputWithBadRequest() throws Exception {
        String token = register("validation@example.test", "RIDER");
        mockMvc.perform(post("/api/rides")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickup\":\"Station\",\"dropoff\":\"Airport\",\"distanceKm\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void appliesPromoDiscountToQuoteAndFinalBookingFare() throws Exception {
        String promo = "TEST" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        promoCodes.save(new PromoCode(promo, new BigDecimal("10.00"), Instant.now().plusSeconds(3600), 2));
        String token = register("promo-" + promo.toLowerCase() + "@example.test", "RIDER");
        String body = objectMapper.writeValueAsString(Map.of(
                "pickup", "Central Station", "dropoff", "Airport", "distanceKm", 10.0, "promoCode", promo));

        MvcResult quote = mockMvc.perform(post("/api/routes/estimate")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode quoteJson = objectMapper.readTree(quote.getResponse().getContentAsString());
        assertThat(quoteJson.get("grossFare").decimalValue()).isEqualByComparingTo("230.00");
        assertThat(quoteJson.get("discountAmount").decimalValue()).isEqualByComparingTo("23.00");
        assertThat(quoteJson.get("estimatedFare").decimalValue()).isEqualByComparingTo("207.00");

        MvcResult booking = mockMvc.perform(post("/api/rides")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode ride = objectMapper.readTree(booking.getResponse().getContentAsString());
        assertThat(ride.get("discountAmount").decimalValue()).isEqualByComparingTo("23.00");
        assertThat(ride.get("estimatedFare").decimalValue()).isEqualByComparingTo("207.00");
        assertThat(ride.get("promoCode").asText()).isEqualTo(promo);
    }

    @Test
    void adminCanOpenAdminDashboardAndPublicSignupCannotCreateAdmins() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Admin", "email", "public-admin@example.test",
                                "password", "correct-horse-battery-staple", "role", "ADMIN"))))
                .andExpect(status().isConflict());

        String email = "admin-" + UUID.randomUUID() + "@example.test";
        users.save(new AppUser("Test Admin", email, passwordEncoder.encode("correct-horse-battery-staple"), UserRole.ADMIN));
        String adminToken = login(email);
        mockMvc.perform(get("/api/admin/metrics").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/rides/me").header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden());
    }

    private String register(String email, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", role + " User", "email", email,
                                "password", "correct-horse-battery-staple", "role", role))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "correct-horse-battery-staple"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
