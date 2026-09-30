package com.intema.demo.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    private final Key signingKey;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret:}") String secret,
                   @Value("${jwt.expiration:86400000}") long expirationMs) {
        if (secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 64
                || secret.startsWith("INSERISCI_") || secret.startsWith("replace-this-demo-secret")) {
            throw new IllegalArgumentException("JWT_SECRET deve essere privato, non un segnaposto, e contenere almeno 64 byte UTF-8");
        }
        if (expirationMs <= 0) throw new IllegalArgumentException("La durata JWT deve essere positiva");
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username) {
        return Jwts.builder().setSubject(username)
                .setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS512).compact();
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey).build()
                .parseClaimsJws(token).getBody().getSubject();
    }
}
