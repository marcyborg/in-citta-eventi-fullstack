package com.intema.demo.security;

import com.intema.demo.model.User;
import com.intema.demo.model.UserRole;
import com.intema.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.boot.DefaultApplicationArguments;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AdminBootstrapTest {
    @Test
    void createsAdminOnlyOfflineAndHashesPassword() {
        UserRepository users = mock(UserRepository.class);
        new AdminBootstrap(users, new MockEnvironment().withProperty("spring.main.web-application-type", "none"),
                "private-admin", "private-password").run(new DefaultApplicationArguments());
        var captured = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captured.capture());
        assertEquals(UserRole.ADMIN, captured.getValue().getRole());
        assertNotEquals("private-password", captured.getValue().getPassword());
    }

    @Test
    void refusesWebModeAndExistingAccounts() {
        UserRepository users = mock(UserRepository.class);
        assertThrows(IllegalStateException.class, () -> new AdminBootstrap(users, new MockEnvironment(),
                "admin", "private-password").run(new DefaultApplicationArguments()));
        when(users.findByUsername("admin")).thenReturn(java.util.Optional.of(new User()));
        assertThrows(IllegalStateException.class, () -> new AdminBootstrap(users,
                new MockEnvironment().withProperty("spring.main.web-application-type", "none"),
                "admin", "private-password").run(new DefaultApplicationArguments()));
        verify(users, never()).saveAndFlush(any());
    }
}
