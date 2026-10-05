package com.bidforge.rfq.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserId")
    private Integer userId;

    @Column(name = "FullName", nullable = false, columnDefinition = "LONGTEXT")
    private String fullName;

    @Column(name = "Email", nullable = false, columnDefinition = "LONGTEXT")
    private String email;

    @Column(name = "PasswordHash", nullable = false, columnDefinition = "LONGTEXT")
    private String passwordHash;

    @Column(name = "Role", nullable = false, length = 30)
    private String role;

    @Column(name = "IsFirstTimeLogin", nullable = false)
    private Boolean firstTimeLogin;
    @Column(name = "SecretKey", columnDefinition = "LONGTEXT")
    private String secretKey;

    @Column(name = "TotpEnabled", nullable = false)
    private boolean totpEnabled = false;

    @Column(name = "TokenVersion", nullable = false)
    private Long tokenVersion = 1L;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public String getTwoFactorSecret() { return secretKey; }
    public void setTwoFactorSecret(String value) { this.secretKey = value; }
}
