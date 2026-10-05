package com.bidforge.rfq.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    @GetMapping({"/", "/login"})
    public String login() { return "auth/login"; }

    @GetMapping("/setup-2fa")
    public String setup() { return "auth/setup-2fa"; }

    @GetMapping("/otp")
    public String otp() { return "auth/verify-otp"; }

    @GetMapping("/lost-otp")
    public String lostOtp() { return "auth/lost-otp"; }

    @GetMapping("/verify-email-otp")
    public String verifyEmailOtp() { return "auth/verify-email-otp"; }

    @GetMapping("/new-2fa")
    public String new2fa() { return "auth/new-2fa"; }
}
