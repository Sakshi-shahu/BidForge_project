package com.bidforge.rfq.service;

import com.bidforge.rfq.entity.User;
import io.jsonwebtoken.Claims;

public interface JwtService {
    String createAccessToken(User user);
    String createRefreshToken(User user);
    String createFlowToken(String email, String purpose, String otpHash);
    Claims parse(String token);
    Claims parseAllowExpired(String token);
    boolean isType(String token, String type);
    boolean isAccessValid(String token, User user);
    boolean isRefreshValid(String token, User user);
    boolean isAccessIdentityValid(String token, User user);
}
