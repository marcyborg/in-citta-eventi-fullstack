package com.intema.demo;

import com.intema.demo.model.Event;
import com.intema.demo.model.User;
import com.intema.demo.model.UserRole;
import com.intema.demo.repository.EventRepository;
import com.intema.demo.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PostgresPersistenceIT {
    @Test
    void eventOwnerAndRoleSurviveRestartWithoutRepeatingMigrations() {
        Long id;
        Long ownerId;
        try (ConfigurableApplicationContext first = start()) {
            User owner = new User();
            owner.setUsername("restart-" + UUID.randomUUID());
            owner.setPassword("fixture-hash");
            ownerId = first.getBean(UserRepository.class).saveAndFlush(owner).getId();
            Event event = new Event();
            event.setTitolo("Persistence regression");
            event.setLuogo("Milano");
            event.setCategoria("sport");
            event.setData(LocalDateTime.now().plusDays(10));
            event.setOwnerId(ownerId);
            id = first.getBean(EventRepository.class).saveAndFlush(event).getId();
            assertEquals(2, first.getBean(Flyway.class).info().applied().length);
        }
        try (ConfigurableApplicationContext second = start()) {
            Event event = second.getBean(EventRepository.class).findById(id).orElseThrow();
            assertEquals("Persistence regression", event.getTitolo());
            assertEquals(ownerId, event.getOwnerId());
            assertEquals(UserRole.USER, second.getBean(UserRepository.class).findById(ownerId).orElseThrow().getRole());
            assertEquals(2, second.getBean(Flyway.class).info().applied().length);
            assertEquals(0, second.getBean(Flyway.class).migrate().migrationsExecuted);
        }
    }

    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(DemoApplication.class).web(WebApplicationType.NONE)
                .profiles("postgres", "test")
                .run("--spring.datasource.url=" + PostgresTestDatabase.url(),
                        "--spring.datasource.username=" + PostgresTestDatabase.user(),
                        "--spring.datasource.password=" + PostgresTestDatabase.password());
    }
}
