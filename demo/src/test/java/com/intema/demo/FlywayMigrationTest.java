package com.intema.demo;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class FlywayMigrationTest {
    @Test
    void versionTwoPreservesUsersAndEventsAndDoesNotAssignOwnersOrAdmins() {
        String url = "jdbc:h2:mem:upgrade-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("1").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(url, "sa", ""));
        jdbc.update("insert into users(username,password) values ('existing','existing-hash')");
        jdbc.update("insert into event(titolo,data,luogo,categoria,latitude,longitude) " +
                "values ('Da conservare',TIMESTAMP '2035-01-01 10:00:00','Milano','sport',45.46,9.19)");
        Flyway flyway = Flyway.configure().dataSource(url, "sa", "").cleanDisabled(true).load();
        assertEquals(1, flyway.migrate().migrationsExecuted);
        assertEquals("USER", jdbc.queryForObject("select role from users", String.class));
        assertEquals("existing-hash", jdbc.queryForObject("select password from users", String.class));
        assertEquals("Da conservare", jdbc.queryForObject("select titolo from event", String.class));
        assertNull(jdbc.queryForObject("select owner_id from event", Long.class));
        assertEquals(45.46, jdbc.queryForObject("select latitude from event", Double.class));
        assertEquals(0, flyway.migrate().migrationsExecuted);
        assertThrows(FlywayException.class, flyway::clean);
    }

    @Test
    void nonEmptyUnmanagedSchemaIsNotAutomaticallyAdopted() {
        String url = "jdbc:h2:mem:unmanaged-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(url, "sa", ""));
        jdbc.execute("create table existing_data(id bigint)");
        jdbc.update("insert into existing_data values (1)");
        assertThrows(FlywayException.class, () -> Flyway.configure().dataSource(url, "sa", "")
                .baselineOnMigrate(false).load().migrate());
        assertEquals(1L, jdbc.queryForObject("select count(*) from existing_data", Long.class));
    }

    @Test
    void explicitBaselineOnAnAuditedLegacyCopyKeepsData() throws Exception {
        String url = "jdbc:h2:mem:baseline-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        var datasource = new DriverManagerDataSource(url, "sa", "");
        try (var connection = datasource.getConnection()) {
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,
                    new org.springframework.core.io.ClassPathResource("db/migration/V1__create_legacy_schema.sql"));
        }
        var jdbc = new JdbcTemplate(datasource);
        jdbc.update("insert into users(username,password) values ('preserved','preserved-hash')");
        jdbc.update("insert into event(titolo,data,luogo,categoria) " +
                "values ('Prima di Flyway',TIMESTAMP '2035-01-01 10:00:00','Milano','teatro')");
        Flyway flyway = Flyway.configure().dataSource(datasource).baselineVersion("1")
                .baselineOnMigrate(false).cleanDisabled(true).load();
        flyway.baseline(); // Explicit operator action on a verified disposable copy, never startup automation.
        assertEquals(1, flyway.migrate().migrationsExecuted);
        assertEquals("preserved-hash", jdbc.queryForObject("select password from users", String.class));
        assertEquals("USER", jdbc.queryForObject("select role from users", String.class));
        assertEquals("Prima di Flyway", jdbc.queryForObject("select titolo from event", String.class));
        assertNull(jdbc.queryForObject("select owner_id from event", Long.class));
    }
}
