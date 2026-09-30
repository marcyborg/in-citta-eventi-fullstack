package com.intema.demo;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PostgresMigrationIT {
    @Test
    void upgradeKeepsHistoricalDataAndEnforcesRoleAndOwnerConstraints() {
        JdbcTemplate jdbc = jdbc();
        String schema = "upgrade_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("create schema " + schema);
        try {
            flyway(schema, "1").migrate();
            jdbc.update("insert into " + schema + ".users(username,password) values ('historical','hash-to-keep')");
            jdbc.update("insert into " + schema + ".event(titolo,data,luogo,categoria) " +
                    "values ('Da conservare',TIMESTAMP '2035-01-01 10:00:00','Milano','teatro')");
            Flyway migrated = flyway(schema, null);
            assertEquals(1, migrated.migrate().migrationsExecuted);
            assertEquals("USER", jdbc.queryForObject("select role from " + schema + ".users", String.class));
            assertEquals("hash-to-keep", jdbc.queryForObject("select password from " + schema + ".users", String.class));
            assertNull(jdbc.queryForObject("select owner_id from " + schema + ".event", Long.class));
            assertEquals("Da conservare", jdbc.queryForObject("select titolo from " + schema + ".event", String.class));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> jdbc.update("update " + schema + ".users set role='SUPERUSER'"));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> jdbc.update("update " + schema + ".event set owner_id=99999"));
            assertEquals(0, migrated.migrate().migrationsExecuted);
        } finally {
            jdbc.execute("drop schema " + schema + " cascade");
        }
    }

    @Test
    void unmanagedPostgresSchemaIsRefusedAndDataIsNotDropped() {
        JdbcTemplate jdbc = jdbc();
        String schema = "unmanaged_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("create schema " + schema);
        try {
            jdbc.execute("create table " + schema + ".existing_data(id bigint)");
            jdbc.update("insert into " + schema + ".existing_data values (1)");
            assertThrows(FlywayException.class, () -> flyway(schema, null).migrate());
            assertEquals(1L, jdbc.queryForObject("select count(*) from " + schema + ".existing_data", Long.class));
        } finally {
            jdbc.execute("drop schema " + schema + " cascade");
        }
    }

    @Test
    void controlledBaselineOnVerifiedLegacySchemaKeepsAccountsAndEvents() throws Exception {
        var datasource = new DriverManagerDataSource(PostgresTestDatabase.url(),
                PostgresTestDatabase.user(), PostgresTestDatabase.password());
        var jdbc = new JdbcTemplate(datasource);
        String schema = "baseline_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("create schema " + schema);
        try {
            try (var connection = datasource.getConnection()) {
                connection.setSchema(schema);
                org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,
                        new org.springframework.core.io.ClassPathResource("db/migration/V1__create_legacy_schema.sql"));
            }
            jdbc.update("insert into " + schema + ".users(username,password) values ('existing','preserved-hash')");
            jdbc.update("insert into " + schema + ".event(titolo,data,luogo,categoria,latitude,longitude) " +
                    "values ('Prima di Flyway',TIMESTAMP '2035-01-01 10:00:00','Milano','sport',45.46,9.19)");
            Flyway flyway = flyway(schema, null);
            flyway.baseline(); // Verified schema and explicit action, not automatic adoption.
            assertEquals(1, flyway.migrate().migrationsExecuted);
            assertEquals("preserved-hash", jdbc.queryForObject("select password from " + schema + ".users", String.class));
            assertEquals("USER", jdbc.queryForObject("select role from " + schema + ".users", String.class));
            assertEquals(45.46, jdbc.queryForObject("select latitude from " + schema + ".event", Double.class));
            assertNull(jdbc.queryForObject("select owner_id from " + schema + ".event", Long.class));
        } finally {
            jdbc.execute("drop schema " + schema + " cascade");
        }
    }

    private Flyway flyway(String schema, String target) {
        var configuration = Flyway.configure().dataSource(PostgresTestDatabase.url(),
                        PostgresTestDatabase.user(), PostgresTestDatabase.password())
                .schemas(schema).defaultSchema(schema).cleanDisabled(true).baselineOnMigrate(false);
        if (target != null) configuration.target(target);
        return configuration.load();
    }

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(new DriverManagerDataSource(PostgresTestDatabase.url(),
                PostgresTestDatabase.user(), PostgresTestDatabase.password()));
    }
}
