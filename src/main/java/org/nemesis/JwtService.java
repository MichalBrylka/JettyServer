package org.nemesis;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtService {

    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(
                    (System.getenv("JWT_SECRET") instanceof String envKey && !envKey.isBlank() ? envKey : "your-secret-key-change-this-in-production")
                            .getBytes(StandardCharsets.UTF_8));

    private static final long EXPIRATION = 1000 * 60 * 60;

    public static String createToken(String username) {

        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(KEY)
                .compact();
    }

    public static Claims parse(String token) {

        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}