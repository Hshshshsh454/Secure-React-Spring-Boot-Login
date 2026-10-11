package com.example.securelogin.auth;

import com.example.securelogin.account.UserAccount;
import com.example.securelogin.account.UserAccountRepository;
import com.example.securelogin.api.dto.UserProfile;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Transactional
    public void register(String normalizedEmail, String passwordHash) {
        userAccountRepository.saveAndFlush(new UserAccount(
                UUID.randomUUID().toString(),
                normalizedEmail,
                passwordHash,
                Instant.now()));
    }

    @Transactional(readOnly = true)
    public UserProfile profileFor(String normalizedEmail) {
        UserAccount account = userAccountRepository.findByEmailNormalized(normalizedEmail)
                .filter(UserAccount::isEnabled)
                .orElseThrow(() -> new AuthenticationServiceException("Authentication required"));
        return UserProfile.from(account);
    }
}
