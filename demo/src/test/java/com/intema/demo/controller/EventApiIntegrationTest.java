package com.intema.demo.controller;

import com.intema.demo.model.Event;
import com.intema.demo.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({"h2", "test"})
@AutoConfigureMockMvc
class EventApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EventRepository repository;
    @Autowired com.intema.demo.repository.UserRepository users;

    @BeforeEach
    void clean() {
        repository.deleteAll();
        users.deleteAll();
        var user = new com.intema.demo.model.User();
        user.setUsername("admin");
        user.setPassword("test-fixture");
        user.setRole(com.intema.demo.model.UserRole.ADMIN);
        users.save(user);
    }

    private Event event(String titolo, String categoria, int days) {
        Event value = new Event();
        value.setTitolo(titolo);
        value.setDescrizione("Descrizione");
        value.setData(LocalDateTime.now().plusDays(days));
        value.setLuogo("Milano");
        value.setCategoria(categoria);
        return repository.save(value);
    }

    @Test
    void filtersAndChronologicalOrder() throws Exception {
        event("Tardi", "teatro", 10);
        event("Presto", "concerto", 2);
        event("Medio", "concerto", 5);
        mvc.perform(get("/api/events").param("categoria", "CONCERTO").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].titolo").value("Presto"))
                .andExpect(jsonPath("$.content[1].titolo").value("Medio"));
        mvc.perform(get("/api/events").param("start", LocalDateTime.now().plusDays(3).toString())
                        .param("end", LocalDateTime.now().plusDays(6).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/events/upcoming"))
                .andExpect(jsonPath("$[0].titolo").value("Presto"))
                .andExpect(jsonPath("$[2].titolo").value("Tardi"));
    }

    @Test
    void errorsAreStructured() throws Exception {
        mvc.perform(get("/api/events/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").exists());
        mvc.perform(get("/api/events").param("start", "2035-01-02T00:00:00")
                        .param("end", "2035-01-01T00:00:00"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").exists());
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createUpdateDeleteAndValidation() throws Exception {
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titolo\":\" \",\"data\":\"2020-01-01T10:00:00\",\"luogo\":\"Milano\",\"categoria\":\"sport\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.titolo").exists())
                .andExpect(jsonPath("$.errors.data").exists());
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titolo\":\"Mappa\",\"data\":\"2099-01-01T10:00:00\","
                                + "\"luogo\":\"Milano\",\"categoria\":\"sport\",\"latitude\":45.46}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.coordinatePairValid").exists());
        Event saved = event("Originale", "sport", 5);
        mvc.perform(put("/api/events/" + saved.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":999,\"titolo\":\"Aggiornato\",\"data\":\"2099-01-01T10:00:00\","
                                + "\"luogo\":\"Milano\",\"categoria\":\"sport\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.titolo").value("Aggiornato"));
        mvc.perform(delete("/api/events/" + saved.getId())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/events/" + saved.getId())).andExpect(status().isNotFound());
    }
}
