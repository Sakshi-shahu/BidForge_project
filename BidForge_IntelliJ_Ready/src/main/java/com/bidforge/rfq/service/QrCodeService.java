package com.bidforge.rfq.service;

public interface QrCodeService {
    String generateDataUri(String email, String secret);
}
