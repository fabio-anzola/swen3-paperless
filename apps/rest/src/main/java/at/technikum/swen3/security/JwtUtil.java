package at.technikum.swen3.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Getter
@Component
public class JwtUtil {

  private final SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

  public String generateToken(String username) {
    long EXPIRATION = 1000 * 60 * 60; // 1 hour
    return Jwts.builder()
        .setSubject(username)
        .claim("username", username)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
        .signWith(key)
        .compact();
  }
}
