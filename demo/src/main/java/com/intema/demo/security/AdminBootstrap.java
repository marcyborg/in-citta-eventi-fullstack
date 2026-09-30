package com.intema.demo.security;

import com.intema.demo.model.User;
import com.intema.demo.model.UserRole;
import com.intema.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/** Explicit offline provisioning only: never grants ADMIN during public registration. */
@Component
@Profile("admin-bootstrap")
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    private final Environment environment;
    private final String username;
    private final String password;

    public AdminBootstrap(UserRepository users, Environment environment,
                          @Value("${ADMIN_USERNAME:}") String username,
                          @Value("${ADMIN_PASSWORD:}") String password) {
        this.users = users;
        this.environment = environment;
        this.username = username.strip();
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!"none".equalsIgnoreCase(environment.getProperty("spring.main.web-application-type"))) {
            throw new IllegalStateException("admin-bootstrap richiede spring.main.web-application-type=none");
        }
        if (username.isBlank() || username.length() > 255 || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Imposta ADMIN_USERNAME e una password privata di almeno 12 caratteri e massimo 72 byte UTF-8");
        }
        if (users.findByUsername(username).isPresent()) {
            throw new IllegalStateException("Username già presente: nessuna promozione automatica di account esistenti");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setRole(UserRole.ADMIN);
        users.saveAndFlush(user);
    }
}
