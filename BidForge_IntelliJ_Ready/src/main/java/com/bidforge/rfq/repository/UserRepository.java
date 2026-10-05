package com.bidforge.rfq.repository;

import com.bidforge.rfq.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmailIgnoreCase(String email);
    List<User> findByRoleIgnoreCase(String role);
}
