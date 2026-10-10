package com.bidforge.rfq.serviceimpl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import com.bidforge.rfq.service.QrCodeService;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class QrCodeServiceImpl implements QrCodeService {

    @Override
    public String generateDataUri(String email, String secret) {

        try {
            String issuer = "JWT-Thymeleaf-Auth";

 String encodedIssuer = encode(issuer);
 String encodedEmail = encode(email);

   String label = encodedIssuer + ":" + encodedEmail;
            String otpUri = "otpauth://totp/" + label+ "?secret=" + secret+ "&issuer=" + encodedIssuer
 + "&algorithm=SHA1" + "&digits=6" + "&period=30";

            var matrix = new MultiFormatWriter().encode(otpUri, BarcodeFormat.QR_CODE, 320,320);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
     MatrixToImageWriter.writeToStream(matrix,"PNG",output);
    String base64 = Base64.getEncoder().encodeToString(output.toByteArray());

            return "data:image/png;base64," + base64;

        } catch (Exception exception) {
            throw new IllegalStateException( "Could not creates Google Authenticator QR code", exception);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode( value, StandardCharsets.UTF_8) .replace("+", "%20");
    }
}
