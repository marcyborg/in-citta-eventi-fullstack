package com.intema.demo.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;

class JwtConfigurationTest {
    @Test
    void missingShortAndPlaceholderSecretsPreventStartup() {
        for (String secret : new String[]{"", "short", "INSERISCI_" + "x".repeat(80),
                "replace-this-demo-secret-before-production-with-a-long-random-64-byte-value"}) {
            new ApplicationContextRunner().withBean(JwtUtil.class, () -> new JwtUtil(secret, 60000))
                    .run(context -> assertNotNull(context.getStartupFailure()));
        }
    }

    @Test
    void validSecretCanSignAndValidateAndRejectsNonPositiveLifetime() {
        JwtUtil jwt = new JwtUtil("random-private-value-".repeat(5), 60000);
        assertEquals("owner", jwt.getUsername(jwt.generateToken("owner")));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("x".repeat(80), 0));
    }
}
