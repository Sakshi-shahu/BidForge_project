package com.bidforge.rfq.controller;

import com.bidforge.rfq.util.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final CurrentUser currentUser;

    public DashboardController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }
        return currentUser.isAdmin() ? "redirect:/admin/dashboard" : "redirect:/vendor/dashboard";
    }
}
