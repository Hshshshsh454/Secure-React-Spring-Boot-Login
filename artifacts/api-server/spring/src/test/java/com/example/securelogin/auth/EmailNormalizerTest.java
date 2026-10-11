package com.example.securelogin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class EmailNormalizerTest {
    @Test
    void trimsAndNormalizesWithoutLocaleSpecificRules() {
        assertEquals("user@example.com", EmailNormalizer.normalize("  User@Example.COM  "));
    }

    @Test
    void preservesNullForRequestValidation() {
        assertNull(EmailNormalizer.normalize(null));
    }
}
