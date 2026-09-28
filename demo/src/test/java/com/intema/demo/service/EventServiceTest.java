package com.intema.demo.service;

import com.intema.demo.model.Event;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("h2")
class EventServiceTest {

    @Autowired
    private EventService eventService;

    @Test
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
