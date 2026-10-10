package com.bidforge.rfq.serviceimpl;

import com.bidforge.rfq.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Value("${app.mail.from}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendOtp(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Your BidForge verification OTP");
        message.setText("Your OTP is " + otp + ". It expires in 5 minute.");
        mailSender.send(message);
        log.info("OTP email sent to: {}", email);
    }

    @Override
    public void sendVendorCredentials(String name, String email, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("BidForge Vendor Account");
        message.setText("Hello " + name + ",\n\nYour BidForge vendor account has been created.\nEmail: " + email
                + "\nTemporary password: " + password
                + "\n\nPlease sign in and complete authenticator setup.");
        mailSender.send(message);
        log.info("Vendor credentials email sent to: {}", email);
    }
}
