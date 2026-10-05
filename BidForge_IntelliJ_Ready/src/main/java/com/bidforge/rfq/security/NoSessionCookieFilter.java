package com.bidforge.rfq.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NoSessionCookieFilter extends OncePerRequestFilter {

    private static final String JSESSIONID = "JSESSIONID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        invalidateExistingSession(request);

        filterChain.doFilter(request, response);


        invalidateExistingSession(request);
        response.addHeader( "Set-Cookie", ResponseCookie.from(JSESSIONID, "").httpOnly(true).path("/")
        .maxAge(0).build().toString());
    }

    private void invalidateExistingSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }
    }
}
