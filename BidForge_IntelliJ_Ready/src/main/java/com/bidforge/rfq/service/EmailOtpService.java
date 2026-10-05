package com.bidforge.rfq.service;

public interface EmailOtpService {
    String generateOtp();
    String hmac(String otp, String email, String secret);
}
