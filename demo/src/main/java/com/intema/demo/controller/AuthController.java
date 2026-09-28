package com.intema.demo.controller;

import com.intema.demo.model.User;
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

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                          UserRepository userRepository, PasswordEncoder encoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    public record Credentials(@NotBlank String username, @NotBlank @Size(min = 8) String password) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Credentials credentials) {
        if (userRepository.findByUsername(credentials.username()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Username già utilizzato"));
        }
        User user = new User();
        user.setUsername(credentials.username());
        user.setPassword(encoder.encode(credentials.password()));
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
                    credentials.username(), credentials.password()));
            return ResponseEntity.ok(Map.of("token", jwtUtil.generateToken(credentials.username())));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenziali non valide"));
        }
    }
}
