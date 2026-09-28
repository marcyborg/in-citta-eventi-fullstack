package com.intema.demo.controller;

import com.intema.demo.model.User;
import com.intema.demo.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        User user = new User();
        user.setUsername("tester");
        user.setPassword(passwordEncoder.encode("password")); // Sempre encoder!
        userRepository.save(user);
        User loaded = userRepository.findByUsername("tester").orElseThrow();
        System.out.println("Test user loaded with encoded password: " + loaded.getPassword());

    }

    private String getJwt() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"" + "tester" + "\", \"password\": \"" + "password" + "\"}"))
                .andReturn().getResponse();

        System.out.println("LOGIN STATUS: " + response.getStatus());
        System.out.println("LOGIN BODY: " + response.getContentAsString());

        if (response.getStatus() != 200) {
            throw new RuntimeException("Login fallito: " + response.getContentAsString());
        }
        return JsonPath.read(response.getContentAsString(), "$.token");
    }


    @Test
    void testCreateEventWithJwtAuth() throws Exception {
        String token = getJwt();

        String eventJson = """
        {
            "titolo": "Concerto",
            "descrizione": "Live show",
            "data": "2030-01-01T21:00:00",
            "luogo": "Teatro Centrale",
            "categoria": "concerti"
        }
        """;

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());


    }

    @Test
    void testCreateEventDeniedWithoutJwt() throws Exception {
        String eventJson = """
        {
            "titolo": "Concerto",
            "descrizione": "Live show",
            "data": "2030-01-01T21:00:00",
            "luogo": "Teatro Centrale",
            "categoria": "concerti"
        }
        """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isUnauthorized());

}}
