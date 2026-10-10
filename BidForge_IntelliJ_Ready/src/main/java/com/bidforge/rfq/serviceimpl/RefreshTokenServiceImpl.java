package com.bidforge.rfq.serviceimpl;

import com.bidforge.rfq.entity.RefreshToken;
import com.bidforge.rfq.entity.User;
import com.bidforge.rfq.repository.RefreshTokenRepository;
import com.bidforge.rfq.service.JwtService;
import com.bidforge.rfq.service.RefreshTokenService;
import com.bidforge.rfq.service.TokenHashService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashService tokenHashService;
    private final JwtService jwtService;
    private final long refreshTokenExpiration;

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            TokenHashService tokenHashService,
            JwtService jwtService,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {

        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHashService = tokenHashService;
        this.jwtService = jwtService;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @Override
    @Transactional
    public void createOrReplace(User user) {

        String refreshToken = jwtService.createRefreshToken(user);
        String refreshTokenHash = tokenHashService.sha256(refreshToken);

        RefreshToken entity = refreshTokenRepository.findByUser(user)
      .orElseGet(() -> RefreshToken.builder().user(user).build());

 entity.setRefreshTokenHash(refreshTokenHash);
entity.setRevoked(false);
entity.setTokenVersion(user.getTokenVersion());
entity.setExpiresAt(Instant.now().plusMillis(refreshTokenExpiration));

        refreshTokenRepository.save(entity);
        log.info("Refresh token stored only in DB for user: {}", user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsable(User user) {

        return refreshTokenRepository.findByUser(user)
                .map(token -> !token.isRevoked() && token.getExpiresAt() != null && token.getExpiresAt().isAfter(Instant.now())
            && user.getTokenVersion().equals(token.getTokenVersion())).orElse(false);
    }

    @Override
    @Transactional
    public void revoke(User user) {

        refreshTokenRepository.findByUser(user).ifPresent(token -> {
 token.setRevoked(true);
refreshTokenRepository.save(token);
            log.info("Refresh token revoked for user: {}", user.getEmail());
        });
    }
}
