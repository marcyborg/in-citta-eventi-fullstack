package com.intema.demo.controller;

import com.intema.demo.model.User;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"h2", "test"})
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;
    @Autowired private com.intema.demo.repository.EventRepository events;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        events.deleteAll();
        userRepository.deleteAll();
        User user = new User();
        user.setUsername("sample");
        user.setPassword(passwordEncoder.encode("strongpass"));
        userRepository.save(user);
    }

    @Test
    void testRegisterAndLoginFlow() throws Exception {
        // Registrazione nuovo utente
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"testuser\", \"password\": \"mypassword\"}"))
                .andExpect(status().isCreated());

        // Login utente esistente
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"testuser\", \"password\": \"mypassword\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

    }
}
