package com.bidforge.rfq.serviceimpl;

import com.bidforge.rfq.dto.*;
import com.bidforge.rfq.entity.User;
import com.bidforge.rfq.exception.InvalidCredentialsException;
import com.bidforge.rfq.exception.InvalidOtpException;
import com.bidforge.rfq.exception.InvalidTokenException;
import com.bidforge.rfq.exception.UserNotFoundException;
import com.bidforge.rfq.repository.UserRepository;
import com.bidforge.rfq.service.*;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

@Service

@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String OLD_REFRESH_COOKIE = "refreshToken";
    private static final long FLOW_COOKIE_SECONDS = 300;
    private static final String RECOVERY_SETUP_PURPOSE = "RECOVERY_SETUP";
    private static final long ACCESS_COOKIE_RETENTION_SECONDS = 604800;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TwoFactorService twoFactorService;
    private final QrCodeService qrCodeService;
    private final EmailOtpService emailOtpService;
    private final EmailService emailService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${app.cookie.secure}")
    private boolean secureCookie;

    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;


    @Override
    public User register(String name, String email) {
        if (name == null || name.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("Vendor name and email are required");
        }

        String vendorName = name.trim();
        if (vendorName.length() < 3) {
            throw new IllegalArgumentException("Vendor name must contain at least 3 characters");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String prefix = vendorName.substring(0, 3);
        String temporaryPassword = prefix + String.format("%03d",
                new java.security.SecureRandom().nextInt(1000));

        User user = new User();
        user.setFullName(vendorName);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setRole("VENDOR");
        user.setFirstTimeLogin(true);
        user.setTotpEnabled(false);
        user.setTokenVersion(1L);

        User saved = userRepository.save(user);

        try {
            emailService.sendVendorCredentials(
                    saved.getFullName(),
                    saved.getEmail(),
                    temporaryPassword);
        } catch (RuntimeException ex) {
            userRepository.delete(saved);
            throw new IllegalStateException(
                    "Vendor was created but credential email could not be sent", ex);
        }

        log.info("Vendor account created for {}. Temporary credentials sent by email.",
                saved.getEmail());

        return saved;
    }

    @Override
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        User user = findUser(request.getEmail());
        verifyPassword(request.getPassword(), user);

        makeSureTokenVersionExists(user);
        clearCookie(response, OLD_REFRESH_COOKIE);

        if (Boolean.TRUE.equals(user.getFirstTimeLogin())) {
            createFirstTimeSecret(user);

            String qrDataUri = qrCodeService.generateDataUri(
                    user.getEmail(), user.getTwoFactorSecret());

            log.info("Password verified. First-time 2FA setup required for: {}", user.getEmail());

            return new LoginResponse(true,
     "Password verified. Scan the QR code and enter the authenticator OTP.", "FIRST_TIME_2FA",
      user.getEmail(), qrDataUri,null);
        }

        log.info("Password verified. Authenticator OTP required for: {}", user.getEmail());

        return new LoginResponse(true,"Password verified. Enter the authenticator OTP.","OTP_REQUIRED",
        user.getEmail(),null,null);
    }

    @Override
    public QrSetupResponse getSetupQr(LoginRequest request) {
        User user = findUser(request.getEmail());
        verifyPassword(request.getPassword(), user);

        if (!Boolean.TRUE.equals(user.getFirstTimeLogin())) {
            throw new InvalidTokenException("2FA setup is not required");
        }

        createFirstTimeSecret(user);

        return new QrSetupResponse(qrCodeService.generateDataUri(
     user.getEmail(), user.getTwoFactorSecret()),user.getTwoFactorSecret());
    }

    @Override
    public void verifyLoginOtp( VerifyLoginOtpRequest request, HttpServletResponse response) {

                User user = findUser(request.getEmail());
        verifyPassword(request.getPassword(), user);

        if (user.getTwoFactorSecret() == null
                || user.getTwoFactorSecret().isBlank()) {
            throw new InvalidTokenException("Authenticator is not configured");
        }

        if (!twoFactorService.verify(
                user.getTwoFactorSecret(), request.getOtp())) {
            log.warn("Invalid authenticator OTP for: {}", user.getEmail());
            throw new InvalidOtpException("Invalid authenticator OTP");
        }

        user.setFirstTimeLogin(false);
        userRepository.save(user);

        issueTokens(user, response);
        log.info("Login completed successfully for: {}", user.getEmail());
    }

    @Override
    public void refreshAccessToken( String accessToken, HttpServletResponse response) {

        if (accessToken == null || accessToken.isBlank()) {
            throw new InvalidTokenException("Access token is missing");
        }

        Claims claims = jwtService.parseAllowExpired(accessToken);

        if (!"ACCESS".equals(claims.get("type", String.class))) {
            throw new InvalidTokenException("Invalid access token");
        }

        User user = findUser(claims.getSubject());

        if (!jwtService.isAccessIdentityValid(accessToken, user)) {
            throw new InvalidTokenException("Access token is invalid or revoked");
        }

        if (!refreshTokenService.isUsable(user)) {
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        String newAccessToken = jwtService.createAccessToken(user);
        addCookie(response, ACCESS_COOKIE,
                newAccessToken, ACCESS_COOKIE_RETENTION_SECONDS);

        log.info("Access token refreshed for: {}", user.getEmail());
    }

    @Override
    public void logout( String accessToken,HttpServletResponse response) {

        User user = null;

        if (accessToken != null && !accessToken.isBlank()) {
            try {
                Claims claims = jwtService.parseAllowExpired(accessToken);
                if ("ACCESS".equals(claims.get("type", String.class))) {
                    user = findUser(claims.getSubject());
                }
            } catch (Exception ignored) {
             
            }
        }

        if (user != null) {
            refreshTokenService.revoke(user);

            long currentVersion = user.getTokenVersion() == null ? 1L : user.getTokenVersion();
            user.setTokenVersion(currentVersion + 1);
            userRepository.save(user);

            log.info("Logout completed for: {}. Token version: {}", user.getEmail(), user.getTokenVersion());
        }

        clearCookie(response, ACCESS_COOKIE);
        clearCookie(response, OLD_REFRESH_COOKIE);
        clearCookie(response, RECOVERY_FLOW_COOKIE);
        clearCookie(response, RECOVERY_SETUP_FLOW_COOKIE);
    }

    @Override
    public String startLostOtp(LoginRequest request,HttpServletResponse response) {

        User user = findUser(request.getEmail());
        verifyPassword(request.getPassword(), user);

        String otp = emailOtpService.generateOtp();
        String otpHash = emailOtpService.hmac( otp, user.getEmail(), jwtSecret);

        String recoveryFlow = jwtService.createFlowToken(user.getEmail(), "EMAIL_OTP", otpHash);

        addCookie(response, RECOVERY_FLOW_COOKIE, recoveryFlow, FLOW_COOKIE_SECONDS);

        emailService.sendOtp(user.getEmail(), otp);
        log.info("Recovery OTP sent to: {}", user.getEmail());
        return "EMAIL_OTP_SENT";
    }

    @Override
    public String verifyLostEmailOtp( OtpRequest request, String recoveryFlow, HttpServletResponse response) {

        Claims claims = getFlowClaims(recoveryFlow);
        checkPurpose(claims, "EMAIL_OTP");

        String expectedHash = claims.get("otpHash", String.class);
        String actualHash = emailOtpService.hmac(request.getOtp(), claims.getSubject(), jwtSecret);

        boolean sameHash = MessageDigest.isEqual( expectedHash.getBytes(StandardCharsets.UTF_8),
                actualHash.getBytes(StandardCharsets.UTF_8));

        if (!sameHash) {
            throw new InvalidOtpException("Invalid email OTP");
        }

        User user = findUser(claims.getSubject());
        user.setTwoFactorSecret(twoFactorService.generateSecret());
        user.setFirstTimeLogin(true);
        userRepository.save(user);

        clearCookie(response, RECOVERY_FLOW_COOKIE);

    
        String recoverySetupFlow = jwtService.createFlowToken( user.getEmail(), RECOVERY_SETUP_PURPOSE, null);

        addCookie(response, RECOVERY_SETUP_FLOW_COOKIE,recoverySetupFlow, FLOW_COOKIE_SECONDS);

        log.info("Email OTP verified. New authenticator setup started for: {}", user.getEmail());

        return "NEW_2FA";
    }

    @Override
    public QrSetupResponse getRecoverySetupQr(String recoverySetupFlow) {

        Claims claims = getFlowClaims(recoverySetupFlow);
        checkPurpose(claims, RECOVERY_SETUP_PURPOSE);

        User user = findUser(claims.getSubject());

        if (user.getTwoFactorSecret() == null
                || user.getTwoFactorSecret().isBlank()) {
            user.setTwoFactorSecret(twoFactorService.generateSecret());
            userRepository.save(user);
        }

        String qrDataUri = qrCodeService.generateDataUri(
                user.getEmail(), user.getTwoFactorSecret());

        return new QrSetupResponse(qrDataUri, null);
    }

    @Override
    public void verifyRecoveryOtp( OtpRequest request, String recoverySetupFlow,HttpServletResponse response) {

        Claims claims = getFlowClaims(recoverySetupFlow);
        checkPurpose(claims, RECOVERY_SETUP_PURPOSE);

        User user = findUser(claims.getSubject());

        if (user.getTwoFactorSecret() == null
                || user.getTwoFactorSecret().isBlank()) {
            throw new InvalidTokenException("Authenticator setup is incomplete");
        }

        if (!twoFactorService.verify(
                user.getTwoFactorSecret(), request.getOtp())) {
            throw new InvalidOtpException("Invalid authenticator OTP");
        }

        user.setFirstTimeLogin(false);
        userRepository.save(user);

        issueTokens(user, response);
        clearCookie(response, RECOVERY_SETUP_FLOW_COOKIE);

        log.info("Lost authenticator recovery completed for: {}", user.getEmail());
    }

    private void issueTokens(User user, HttpServletResponse response) {
        String accessToken = jwtService.createAccessToken(user);
        refreshTokenService.createOrReplace(user);

        clearCookie(response, OLD_REFRESH_COOKIE);
        addCookie(response, ACCESS_COOKIE,accessToken, ACCESS_COOKIE_RETENTION_SECONDS);
    }

    private void createFirstTimeSecret(User user) {
        if (user.getTwoFactorSecret() == null|| user.getTwoFactorSecret().isBlank()) {
            user.setTwoFactorSecret(twoFactorService.generateSecret());
            userRepository.save(user);
        }
    }

    private void verifyPassword(String password, User user) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Invalid password for email: {}", user.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    private Claims getFlowClaims(String flowToken) {
        if (flowToken == null || flowToken.isBlank()) {
            throw new InvalidTokenException("Recovery flow has expired");
        }

        Claims claims = jwtService.parse(flowToken);

        if (!"FLOW".equals(claims.get("type", String.class))) {
            throw new InvalidTokenException("Invalid recovery token");
        }

        return claims;
    }

    private void checkPurpose(Claims claims, String... allowedPurposes) {
        String purpose = claims.get("purpose", String.class);

        for (String allowedPurpose : allowedPurposes) {
            if (allowedPurpose.equals(purpose)) {
                return;
            }
        }

        throw new InvalidTokenException("Invalid recovery flow");
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private void makeSureTokenVersionExists(User user) {
        if (user.getTokenVersion() == null) {
            user.setTokenVersion(1L);
            userRepository.save(user);
        }
    }

    private void addCookie(HttpServletResponse response, String name,String value,long maxAgeSeconds) {

        ResponseCookie cookie = ResponseCookie.from(name, value).httpOnly(true).secure(secureCookie)
                .sameSite(sameSite).path("/").maxAge(Duration.ofSeconds(maxAgeSeconds)).build();
    response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearCookie( HttpServletResponse response, String name) {

        ResponseCookie cookie = ResponseCookie.from(name, "") .httpOnly(true).secure(secureCookie) .sameSite(sameSite)
                .path("/").maxAge(Duration.ZERO).build();
response.addHeader("Set-Cookie", cookie.toString());
    }
}
