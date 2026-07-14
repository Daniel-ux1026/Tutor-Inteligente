package pe.edu.utp.tutor.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.utp.tutor.domain.UserEntity;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessMinutes;
    private final long refreshDays;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-minutes}") long accessMinutes,
                      @Value("${app.jwt.refresh-days}") long refreshDays) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessMinutes = accessMinutes;
        this.refreshDays = refreshDays;
    }

    public String accessToken(UserEntity user) { return token(user, "access", Instant.now().plus(accessMinutes, ChronoUnit.MINUTES)); }
    public String refreshToken(UserEntity user) { return token(user, "refresh", Instant.now().plus(refreshDays, ChronoUnit.DAYS)); }
    public long accessSeconds() { return accessMinutes * 60; }

    private String token(UserEntity user, String type, Instant expiration) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getEmail())
            .claim("role", user.getRole().getName()).claim("type", type)
            .issuedAt(Date.from(now)).expiration(Date.from(expiration)).signWith(key).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
