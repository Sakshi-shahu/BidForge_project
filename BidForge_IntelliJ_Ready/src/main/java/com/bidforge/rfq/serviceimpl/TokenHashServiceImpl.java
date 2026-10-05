package com.bidforge.rfq.serviceimpl;

import org.springframework.stereotype.Service;

import com.bidforge.rfq.service.TokenHashService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class TokenHashServiceImpl implements TokenHashService {

    @Override
    public String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
 return HexFormat.of().formatHex(hash);

        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
