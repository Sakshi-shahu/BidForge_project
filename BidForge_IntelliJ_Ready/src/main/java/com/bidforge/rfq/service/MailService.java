package com.bidforge.rfq.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailService(JavaMailSender mailSender,
                       @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendVendorCredentials(String vendorName, String email, String password) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(email);
            helper.setSubject("BidForge Vendor Account");
            helper.setText("Hello " + vendorName + ",\n\n"
                    + "Your BidForge vendor account has been created by the Admin.\n\n"
                    + "Login URL: http://localhost:8080/login\n"
                    + "Email: " + email + "\n"
                    + "Temporary Password: " + password + "\n\n"
                    + "On your first login, BidForge will ask you to configure Google Authenticator.\n\n"
                    + "Please change the temporary password after your first successful login.", false);
            mailSender.send(message);
        } catch (MessagingException | RuntimeException ex) {
            throw new IllegalStateException("Vendor was not emailed. Check SMTP configuration and try again.", ex);
        }
    }

    public void sendRecoveryOtp(String email, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(email);
            helper.setSubject("BidForge 2FA Recovery OTP");
            helper.setText("Your BidForge 2FA recovery OTP is: " + otp
                    + "\n\nThis OTP expires in 10 minutes.\nIf you did not request recovery, ignore this email.", false);
            mailSender.send(message);
        } catch (MessagingException | RuntimeException ex) {
            throw new IllegalStateException("Unable to send recovery OTP email. Check SMTP configuration.", ex);
        }
    }
}
