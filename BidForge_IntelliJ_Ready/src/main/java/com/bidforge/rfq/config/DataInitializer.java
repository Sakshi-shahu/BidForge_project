package com.bidforge.rfq.config;

import com.bidforge.rfq.entity.User;
import com.bidforge.rfq.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final String ADMIN_EMAIL = "sakshishahu81@gmail.com";
    private static final String ADMIN_PASSWORD = "sakshi";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.seed.enabled:true}") boolean enabled) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        createOrUpdateAdmin();
    }

    private void createOrUpdateAdmin() {
        List<User> admins = userRepository.findByRoleIgnoreCase("ADMIN");
        User admin = admins.stream().findFirst().orElse(null);

        if (admin == null) {
            admin = new User();
            admin.setFullName("BidForge Admin");
            admin.setEmail(ADMIN_EMAIL);
            admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setRole("ADMIN");
            admin.setFirstTimeLogin(true);
            admin.setTotpEnabled(false);
            userRepository.save(admin);
            return;
        }

        User existingWithNewEmail = userRepository.findByEmailIgnoreCase(ADMIN_EMAIL).orElse(null);
        if (existingWithNewEmail != null && !existingWithNewEmail.getUserId().equals(admin.getUserId())) {
            throw new IllegalStateException("The admin email " + ADMIN_EMAIL + " is already assigned to another user.");
        }

        boolean changed = false;
        if (!ADMIN_EMAIL.equalsIgnoreCase(admin.getEmail())) {
            admin.setEmail(ADMIN_EMAIL);
            changed = true;
        }
        if (admin.getPasswordHash() == null || admin.getPasswordHash().isBlank()) {
            admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            changed = true;
        }

        if (changed) {
            userRepository.save(admin);
        }
    }
}
