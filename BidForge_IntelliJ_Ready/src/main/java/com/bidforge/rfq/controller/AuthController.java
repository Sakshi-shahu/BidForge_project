package com.bidforge.rfq.controller;

import com.bidforge.rfq.dto.LoginRequest;
import com.bidforge.rfq.dto.OtpRequest;
import com.bidforge.rfq.dto.QrSetupResponse;
import com.bidforge.rfq.dto.VerifyLoginOtpRequest;
import com.bidforge.rfq.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        HttpServletResponse response, Model model) {
        try {
            var result = authService.login(new LoginRequest(email, password), response);
            model.addAttribute("email", email);
            model.addAttribute("password", password);
            if ("FIRST_TIME_2FA".equals(result.getNext())) {
                model.addAttribute("qrDataUri", result.getQrDataUri());
                return "auth/setup-2fa";
            }
            return "auth/verify-otp";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/login";
        }
    }

    @GetMapping("/setup-qr")
    public String setupQr() { return "redirect:/login"; }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String email, @RequestParam String password,
                            @RequestParam String otp, HttpServletResponse response, Model model) {
        try {
            authService.verifyLoginOtp(new VerifyLoginOtpRequest(email, password, otp), response);
            return "redirect:/dashboard";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("password", password);
            return "auth/verify-otp";
        }
    }

    @GetMapping("/refresh")
    public String refresh(HttpServletRequest request, HttpServletResponse response) {
        authService.refreshAccessToken(cookie(request, AuthService.ACCESS_COOKIE), response);
        return "redirect:/dashboard";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(cookieOrNull(request, AuthService.ACCESS_COOKIE), response);
        return "redirect:/login";
    }

    @PostMapping("/lost-otp")
    public String lostOtp(@RequestParam String email, @RequestParam String password,
                          HttpServletResponse response, Model model) {
        try {
            authService.startLostOtp(new LoginRequest(email, password), response);
            return "redirect:/verify-email-otp";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/lost-otp";
        }
    }

    @PostMapping("/verify-email-otp")
    public String verifyEmailOtp(@RequestParam String otp, HttpServletRequest request,
                                 HttpServletResponse response, Model model) {
        try {
            String result = authService.verifyLostEmailOtp(new OtpRequest(otp), cookie(request, AuthService.RECOVERY_FLOW_COOKIE), response);
            if ("NEW_2FA".equals(result)) {
                QrSetupResponse qr = authService.getRecoverySetupQr(cookie(request, AuthService.RECOVERY_SETUP_FLOW_COOKIE));
                model.addAttribute("qrDataUri", qr.getQrDataUri());
                return "auth/new-2fa";
            }
            model.addAttribute("error", "Invalid recovery response");
            return "auth/verify-email-otp";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/verify-email-otp";
        }
    }

    @GetMapping("/recovery-setup-qr")
    public String recoveryQr(HttpServletRequest request, Model model) {
        try {
            QrSetupResponse qr = authService.getRecoverySetupQr(cookie(request, AuthService.RECOVERY_SETUP_FLOW_COOKIE));
            model.addAttribute("qrDataUri", qr.getQrDataUri());
            return "auth/new-2fa";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/lost-otp";
        }
    }

    @PostMapping("/verify-recovery-otp")
    public String verifyRecoveryOtp(@RequestParam String otp, HttpServletRequest request,
                                    HttpServletResponse response, Model model) {
        try {
            authService.verifyRecoveryOtp(new OtpRequest(otp), cookie(request, AuthService.RECOVERY_SETUP_FLOW_COOKIE), response);
            return "redirect:/dashboard";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/new-2fa";
        }
    }

    private String cookie(HttpServletRequest request, String name) {
        String value = cookieOrNull(request, name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Required cookie is missing or expired");
        return value;
    }

    private String cookieOrNull(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) if (name.equals(c.getName())) return c.getValue();
        return null;
    }

}
