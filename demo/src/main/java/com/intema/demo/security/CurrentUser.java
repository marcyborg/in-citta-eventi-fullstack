package com.intema.demo.security;

import com.intema.demo.model.User;
import com.intema.demo.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User require() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Autenticazione richiesta");
        }
        // Ownership and roles are resolved from the database, not request bodies
        // or stale role claims embedded in a JWT.
        return users.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Account non disponibile"));
    }
}
