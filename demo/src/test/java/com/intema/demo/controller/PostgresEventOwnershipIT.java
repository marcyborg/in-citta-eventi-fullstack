package com.intema.demo.controller;

import com.intema.demo.PostgresTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles(value = {"postgres", "test"}, inheritProfiles = false)
class PostgresEventOwnershipIT extends EventOwnershipTest {
    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", PostgresTestDatabase::url);
        properties.add("spring.datasource.username", PostgresTestDatabase::user);
        properties.add("spring.datasource.password", PostgresTestDatabase::password);
    }
}
