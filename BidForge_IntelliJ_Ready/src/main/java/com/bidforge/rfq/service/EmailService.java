package com.bidforge.rfq.service;

public interface EmailService {
    void sendOtp(String email, String otp);
    void sendVendorCredentials(String name, String email, String password);
}
