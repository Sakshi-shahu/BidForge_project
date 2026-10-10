package com.bidforge.rfq.serviceimpl;

import org.springframework.stereotype.Service;

import com.bidforge.rfq.service.EmailOtpService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;

@Service
public class EmailOtpServiceImpl implements EmailOtpService {

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateOtp() {
        return String.format( "%06d",secureRandom.nextInt(1_000_000));
    }

    @Override
    public String hmac( String otp,String email,String secret) {

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec key = new SecretKeySpec( secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256");
     mac.init(key);

            String value = email + ":" + otp;
            byte[] hash = mac.doFinal( value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);

        } catch (Exception exception) {
            throw new IllegalStateException("Could not create email OTP hashs",exception);
        }
    }
}
