package com.bidforge.rfq.security;

import com.bidforge.rfq.entity.User;
import com.bidforge.rfq.repository.UserRepository;
import com.bidforge.rfq.service.AuthService;
import com.bidforge.rfq.service.JwtService;
import com.bidforge.rfq.service.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final AuthService authService;

    public JwtAuthenticationFilter(
    		JwtService jwtService,
            UserRepository userRepository,
            RefreshTokenService refreshTokenService,
            AuthService authService) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String accessToken = readCookie(
                request,
                AuthService.ACCESS_COOKIE);

        if (accessToken != null) {
            authenticateWithAccessToken(accessToken);
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            tryRefreshAccessToken(accessToken, response);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateWithAccessToken(String accessToken) {
        try {
            var claims = jwtService.parse(accessToken);

            User user = userRepository
                    .findByEmailIgnoreCase(claims.getSubject())
                    .orElse(null);

            if (user == null) {
                return;
            }

            if (jwtService.isAccessValid(accessToken, user)) {
                setAuthentication(user);
            }

        } catch (Exception exception) {
            log.debug("Access token could not authenticate: {}", exception.getMessage());
        }
    }

    private void tryRefreshAccessToken(
            String accessToken,
            HttpServletResponse response) {

        if (accessToken == null || accessToken.isBlank()) {
            return;
        }

        try {
            var claims = jwtService.parseAllowExpired(accessToken);

            if (!"ACCESS".equals(claims.get("type", String.class))) {
                return;
            }

            User user = userRepository.findByEmailIgnoreCase(claims.getSubject()).orElse(null);
                   

            if (user == null) {
                return;
            }

            if (!jwtService.isAccessIdentityValid(accessToken, user)) {
                return;
            }

            if (!refreshTokenService.isUsable(user)) {
                return;
            }

            authService.refreshAccessToken(accessToken, response);
            setAuthentication(user);

        } catch (Exception exception) {
            log.debug("DB refresh could not authenticate request: {}", exception.getMessage());
        }
    }

    private void setAuthentication(User user) {
        SimpleGrantedAuthority authority =new SimpleGrantedAuthority("ROLE_" + user.getRole());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        		user.getEmail(), null,List.of(authority));      
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String readCookie(HttpServletRequest request, String cookieName) {

        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
