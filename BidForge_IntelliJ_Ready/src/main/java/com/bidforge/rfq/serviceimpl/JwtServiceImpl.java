package com.bidforge.rfq.serviceimpl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.bidforge.rfq.service.JwtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bidforge.rfq.entity.User;
import com.bidforge.rfq.exception.InvalidTokenException;
import com.bidforge.rfq.exception.TokenExpiredException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtServiceImpl implements JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtServiceImpl.class);

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final long flowTokenExpiration;

    public JwtServiceImpl(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,
            @Value("${jwt.flow-token-expiration}") long flowTokenExpiration) {

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8));

        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.flowTokenExpiration = flowTokenExpiration;
    }

    @Override
    public String createAccessToken(User user) {
        return createToken( user.getEmail(), "ACCESS", user.getTokenVersion(), accessTokenExpiration);
    }

    @Override
    public String createRefreshToken(User user) {
        return createToken( user.getEmail(), "REFRESH", user.getTokenVersion(),refreshTokenExpiration);
    }

    @Override
    public String createFlowToken( String email,String purpose, String otpHash) {

        return Jwts.builder().subject(email).claim("type", "FLOW").claim("purpose", purpose)
            .claim("otpHash", otpHash).id(UUID.randomUUID().toString()).issuedAt(new Date())
         .expiration(new Date( System.currentTimeMillis() + flowTokenExpiration))
          .signWith(secretKey) .compact();
    }

    private String createToken(String email, String tokenType, Long tokenVersion,long expiration) {

        return Jwts.builder().subject(email).claim("type", tokenType) .claim("tokenVersion", tokenVersion)
                .id(UUID.randomUUID().toString()) .issuedAt(new Date()) .expiration(new Date(
 System.currentTimeMillis() + expiration)).signWith(secretKey).compact();
    }

    @Override
    public Claims parse(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token is missing");
        }

        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();

        } catch (ExpiredJwtException exception) {
            throw new TokenExpiredException("Token has expired");

        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidTokenException("Invalid tokens");
        }
    }

    @Override
    public Claims parseAllowExpired(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token is missing");
        }

        try {
            return Jwts.parser().verifyWith(secretKey) .build() .parseSignedClaims(token).getPayload();

        } catch (ExpiredJwtException exception) {
            return exception.getClaims();

        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidTokenException("Invalid token");
        }
    }

    @Override
    public boolean isType(String token, String type) {
        try {
            return type.equals(parse(token).get("type", String.class));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public boolean isAccessValid(String token, User user) {
        try {
            Claims claims = parse(token);

            return "ACCESS".equals(claims.get("type", String.class))
     && user.getEmail().equalsIgnoreCase(claims.getSubject())
      && user.getTokenVersion().equals(claims.get("tokenVersion", Long.class));

        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public boolean isRefreshValid(String token, User user) {
        try {
            Claims claims = parse(token);

            return "REFRESH".equals(claims.get("type", String.class))
   && user.getEmail().equalsIgnoreCase(claims.getSubject())
     && user.getTokenVersion().equals(claims.get("tokenVersion", Long.class));

        } catch (RuntimeException exception) {
            return false;
        }
    }
    @Override
    public boolean isAccessIdentityValid(String token, User user) {
        try {
            Claims claims = parseAllowExpired(token);
            Long tokenVersion = claims.get("tokenVersion", Long.class);

            return "ACCESS".equals(claims.get("type", String.class))
   && user.getEmail().equalsIgnoreCase(claims.getSubject())
 && user.getTokenVersion() != null
 && user.getTokenVersion().equals(tokenVersion);

        } catch (RuntimeException exception) {
            return false;
        }
    }

}
