package com.bidforge.rfq.serviceimpl;

import org.springframework.stereotype.Service;

import com.bidforge.rfq.service.TwoFactorService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

@Service
public class TwoFactorServiceImpl implements TwoFactorService {

    private static final String BASE32_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private static final long TIME_STEP_SECONDS = 30L;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateSecret() {

        byte[] bytes = new byte[20];
        secureRandom.nextBytes(bytes);

        return encodeBase32(bytes);
    }

    @Override
    public boolean verify(String secret, String otp) {

        if (secret == null || secret.isBlank()
                || otp == null || !otp.matches("\\d{6}")) {
            return false;
        }

        long currentCounter = System.currentTimeMillis() / 1000L/ TIME_STEP_SECONDS;
        for (long counter = currentCounter - 1; counter <= currentCounter + 1; counter++) {

            if (generateCode(secret, counter).equals(otp)) {
                return true;
            }
        }

        return false;
    }

    private String generateCode(String secret, long counter) {
        try {
            byte[] key = decodeBase32(secret);
   byte[] data = ByteBuffer .allocate(8) .putLong(counter).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));

            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;

            int binary = ((hash[offset] & 0x7F) << 24)| ((hash[offset + 1] & 0xFF) << 16) | ((hash[offset + 2] & 0xFF) << 8)
       | (hash[offset + 3] & 0xFF);

            return String.format("%06d", binary % 1_000_000);

        } catch (Exception exception) {
            throw new IllegalStateException( "Could not generate authenticator OTP", exception);
        }
    }

    private String encodeBase32(byte[] data) {

        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsInBuffer = 0;

        for (byte value : data) {
            buffer = (buffer << 8) | (value & 0xFF);
            bitsInBuffer += 8;

            while (bitsInBuffer >= 5) {
           bitsInBuffer -= 5;
         int index = (buffer >> bitsInBuffer) & 31;
      result.append(BASE32_ALPHABET.charAt(index));
            }
        }

        if (bitsInBuffer > 0) {
            int index = (buffer << (5 - bitsInBuffer)) & 31;
            result.append(BASE32_ALPHABET.charAt(index));
        }

        return result.toString();
    }

    private byte[] decodeBase32(String value) {

        String cleanValue = value.replace("=", "").replace(" ", "") .replace("-", "").toUpperCase();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int buffer = 0;
        int bitsInBuffer = 0;

        for (char character : cleanValue.toCharArray()) {
            int valueIndex = BASE32_ALPHABET.indexOf(character);

            if (valueIndex < 0) { 
            	throw new IllegalArgumentException( "Invalid Base32 secret");
            }

            buffer = (buffer << 5) | valueIndex;
            bitsInBuffer += 5;

            if (bitsInBuffer >= 8) {
                bitsInBuffer -= 8; output.write((buffer >> bitsInBuffer) & 0xFF);
            }
        }

        return output.toByteArray();
    }
}
