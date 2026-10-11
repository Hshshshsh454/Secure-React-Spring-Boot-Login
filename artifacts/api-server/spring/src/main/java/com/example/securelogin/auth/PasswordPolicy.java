package com.example.securelogin.auth;

import com.example.securelogin.api.InvalidRequestException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    private static final int MAX_BCRYPT_BYTES = 72;

    public void validate(String password) {
        if (password == null
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            throw new InvalidRequestException("Password exceeds the supported length.");
        }
    }
}
