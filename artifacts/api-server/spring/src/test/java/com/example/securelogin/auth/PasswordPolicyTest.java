package com.example.securelogin.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.securelogin.api.InvalidRequestException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {
    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Test
    void acceptsPasswordsWithinBcryptUtf8ByteLimit() {
        assertDoesNotThrow(() -> passwordPolicy.validate("é".repeat(36)));
    }

    @Test
    void rejectsPasswordsThatWouldBeTruncatedByBcrypt() {
        assertThrows(
                InvalidRequestException.class,
                () -> passwordPolicy.validate("é".repeat(37)));
    }
}
