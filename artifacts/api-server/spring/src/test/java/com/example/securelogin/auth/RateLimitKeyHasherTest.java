package com.example.securelogin.auth;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RateLimitKeyHasherTest {
    private static final String TEST_SECRET = "unit-test-secret-with-at-least-32-bytes";

    @Test
    void isDeterministicForTheSameScopeAndIdentifier() {
        RateLimitKeyHasher hasher = new RateLimitKeyHasher(TEST_SECRET);
        assertArrayEquals(
                hasher.hash("login", "user@example.com"),
                hasher.hash("login", "user@example.com"));
    }

    @Test
    void separatesScopesAndIdentifiers() {
        RateLimitKeyHasher hasher = new RateLimitKeyHasher(TEST_SECRET);
        byte[] login = hasher.hash("login", "user@example.com");
        byte[] registration = hasher.hash("register", "user@example.com");
        byte[] otherEmail = hasher.hash("login", "other@example.com");

        assertFalse(Arrays.equals(login, registration));
        assertFalse(Arrays.equals(login, otherEmail));
    }

    @Test
    void rejectsShortSecrets() {
        assertThrows(IllegalStateException.class, () -> new RateLimitKeyHasher("too-short"));
    }
}
