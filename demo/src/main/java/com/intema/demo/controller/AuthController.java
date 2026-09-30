package com.intema.demo.controller;

import com.intema.demo.model.User;
import com.intema.demo.model.UserRole;
import com.intema.demo.security.CurrentUser;
import com.intema.demo.repository.UserRepository;
import com.intema.demo.security.JwtUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.nio.charset.StandardCharsets;
import jakarta.validation.constraints.AssertTrue;

@RestController
@RequestMapping("/api/auth")
@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(
        type = org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final CurrentUser currentUser;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                          UserRepository userRepository, PasswordEncoder encoder, CurrentUser currentUser) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.currentUser = currentUser;
    }

    public record Credentials(@NotBlank @Size(max = 255) String username,
                              @NotBlank @Size(min = 8, max = 72) String password) {
        @AssertTrue(message = "La password non deve superare 72 byte UTF-8")
        public boolean isPasswordByteLengthValid() {
            return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
        }
    }

    public record Profile(Long id, String username, UserRole role) {
        static Profile of(User user) { return new Profile(user.getId(), user.getUsername(), user.getRole()); }
    }

    @GetMapping("/me")
    public Profile me() {
        return Profile.of(currentUser.require());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Credentials credentials) {
        String username = credentials.username().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Username già utilizzato"));
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(credentials.password()));
        user.setRole(UserRole.USER);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Username già utilizzato"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Utente registrato"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody Credentials credentials) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    credentials.username().trim(), credentials.password()));
            User user = userRepository.findByUsername(credentials.username().trim()).orElseThrow();
            return ResponseEntity.ok(Map.of("token", jwtUtil.generateToken(user.getUsername()), "user", Profile.of(user)));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenziali non valide"));
        }
    }
}
