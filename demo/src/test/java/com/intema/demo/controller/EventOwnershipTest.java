package com.intema.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intema.demo.model.Event;
import com.intema.demo.model.User;
import com.intema.demo.model.UserRole;
import com.intema.demo.repository.EventRepository;
import com.intema.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"h2", "test"})
public class EventOwnershipTest {
    @Autowired protected MockMvc mvc;
    @Autowired protected UserRepository users;
    @Autowired protected EventRepository events;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected ObjectMapper mapper;

    protected User owner;
    protected User other;
    protected User admin;
    protected String ownerToken;
    protected String otherToken;
    protected String adminToken;
    protected Event ownedEvent;
    protected Event legacyEvent;

    @BeforeEach
    void fixtures() throws Exception {
        events.deleteAll();
        users.deleteAll();
        owner = account("owner", UserRole.USER);
        other = account("other", UserRole.USER);
        admin = account("admin", UserRole.ADMIN);
        ownerToken = login(owner.getUsername());
        otherToken = login(other.getUsername());
        adminToken = login(admin.getUsername());
        ownedEvent = event(owner.getId());
        legacyEvent = event(null);
    }

    protected User account(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode("fixture-password"));
        user.setRole(role);
        return users.saveAndFlush(user);
    }

    protected String login(String username) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of("username", username, "password", "fixture-password"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("token").asText();
    }

    protected Event event(Long ownerId) {
        Event event = new Event();
        event.setTitolo("Evento da conservare");
        event.setData(LocalDateTime.now().plusDays(15));
        event.setLuogo("Milano");
        event.setCategoria("teatro");
        event.setOwnerId(ownerId);
        return events.saveAndFlush(event);
    }

    protected String payload(Long requestedOwner) {
        return """
                {"titolo":"Aggiornato","descrizione":"Prova","data":"2099-01-01T10:00:00",
                 "luogo":"Milano","categoria":"teatro","ownerId":%s}
                """.formatted(requestedOwner == null ? "null" : requestedOwner);
    }

    @Test
    void publicReadsAndAnonymousWrites() throws Exception {
        mvc.perform(get("/api/events/" + ownedEvent.getId())).andExpect(status().isOk());
        mvc.perform(get("/api/events").param("categoria", "teatro")).andExpect(status().isOk());
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(payload(null)))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/events/" + ownedEvent.getId()).contentType(MediaType.APPLICATION_JSON).content(payload(null)))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/events/" + ownedEvent.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void creationUsesAuthenticatedOwnerAndIgnoresSpoofedOwnerId() throws Exception {
        mvc.perform(post("/api/events").header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload(other.getId())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ownerId").value(owner.getId()));
    }

    @Test
    void searchWorksWithAbsentAndSingleEndedDateFilters() throws Exception {
        mvc.perform(get("/api/events")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/events").param("categoria", "TEATRO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/events").param("start", LocalDateTime.now().plusDays(1).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/events").param("end", LocalDateTime.now().plusDays(1).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/events").param("start", LocalDateTime.now().plusDays(1).toString())
                        .param("end", LocalDateTime.now().plusDays(20).toString()).param("categoria", "teatro"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void anotherUserCannotUpdateOrDeleteAndDataIsUnchanged() throws Exception {
        mvc.perform(put("/api/events/" + ownedEvent.getId()).header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload(other.getId())))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(delete("/api/events/" + ownedEvent.getId()).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
        assertEquals("Evento da conservare", events.findById(ownedEvent.getId()).orElseThrow().getTitolo());
    }

    @Test
    void ownerCanUpdateAndDeleteButCannotTransferOwnership() throws Exception {
        mvc.perform(put("/api/events/" + ownedEvent.getId()).header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload(other.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value(owner.getId()));
        mvc.perform(delete("/api/events/" + ownedEvent.getId()).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
        assertFalse(events.existsById(ownedEvent.getId()));
    }

    @Test
    void administratorCanManageOthersEventsWithoutChangingTheirOwner() throws Exception {
        mvc.perform(put("/api/events/" + ownedEvent.getId()).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload(admin.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value(owner.getId()));
        mvc.perform(delete("/api/events/" + ownedEvent.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void historicalEventsRemainOwnerlessAndAreManageableOnlyByAdmin() throws Exception {
        mvc.perform(put("/api/events/" + legacyEvent.getId()).header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload(owner.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/events/" + legacyEvent.getId()).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/events/" + legacyEvent.getId()).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload(admin.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").isEmpty());
        mvc.perform(delete("/api/events/" + legacyEvent.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void publicRegistrationCannotChooseAdminRole() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newuser","password":"fixture-password","role":"ADMIN","id":999}
                                """))
                .andExpect(status().isCreated());
        assertEquals(UserRole.USER, users.findByUsername("newuser").orElseThrow().getRole());
    }

    @Test
    void profileDoesNotExposePasswordAndDemotionAppliesToExistingToken() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
        admin.setRole(UserRole.USER);
        users.saveAndFlush(admin);
        mvc.perform(delete("/api/events/" + ownedEvent.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidExpiredAndDeletedAccountTokensAreRejected() throws Exception {
        mvc.perform(delete("/api/events/" + ownedEvent.getId()).header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized());
        var secret = "CI-ONLY-public-fixture-for-tests-abcdefghijklmnopqrstuvwxyz-0123456789-abcdef";
        String expired = io.jsonwebtoken.Jwts.builder().setSubject("owner")
                .setExpiration(new java.util.Date(System.currentTimeMillis() - 10000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        io.jsonwebtoken.SignatureAlgorithm.HS512).compact();
        mvc.perform(delete("/api/events/" + ownedEvent.getId()).header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
        users.delete(other);
        mvc.perform(post("/api/events").header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload(null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void utf8PasswordOverBcryptByteLimitIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of("username", "unicode", "password", "é".repeat(40)))))
                .andExpect(status().isBadRequest());
    }
}
