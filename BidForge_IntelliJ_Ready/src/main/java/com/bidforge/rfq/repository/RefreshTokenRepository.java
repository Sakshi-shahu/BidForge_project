package com.bidforge.rfq.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bidforge.rfq.entity.RefreshToken;
import com.bidforge.rfq.entity.User;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUser(User user);
}
