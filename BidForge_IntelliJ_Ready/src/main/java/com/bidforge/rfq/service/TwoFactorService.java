package com.bidforge.rfq.service;

public interface TwoFactorService {
    String generateSecret();
    boolean verify(String secret, String otp);
}
