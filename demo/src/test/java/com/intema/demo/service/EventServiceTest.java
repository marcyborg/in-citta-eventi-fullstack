package com.intema.demo.service;

import com.intema.demo.model.Event;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles({"h2", "test"})
class EventServiceTest {

    @Autowired
    private EventService eventService;
    @Autowired private com.intema.demo.repository.UserRepository users;
    @Autowired private com.intema.demo.repository.EventRepository events;

    @org.junit.jupiter.api.BeforeEach
    void prepareOwner() {
        events.deleteAll();
        users.deleteAll();
        var owner = new com.intema.demo.model.User();
        owner.setUsername("service-owner");
        owner.setPassword("test-fixture");
        users.save(owner);
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "service-owner")
    void testCreateEvent() {
        Event event = new Event();
        event.setTitolo("Concerto");
        event.setCategoria("concerti");
        event.setDescrizione("Live show");
        event.setData(LocalDateTime.now().plusDays(1));
        event.setLuogo("Teatro Centrale");
        Event saved = eventService.saveEvent(event);
        assertNotNull(saved.getId());
    }
}
