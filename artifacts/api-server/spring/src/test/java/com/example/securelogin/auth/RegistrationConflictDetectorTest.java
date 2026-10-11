package com.example.securelogin.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class RegistrationConflictDetectorTest {
    @Test
    void identifiesMysqlDuplicateKeyErrorsAnywhereInTheCauseChain() {
        RuntimeException wrapper = new RuntimeException(
                "wrapped", new SQLException("duplicate", "23000", 1062));
        assertTrue(RegistrationConflictDetector.isDuplicateKey(wrapper));
    }

    @Test
    void doesNotHideOtherDatabaseErrors() {
        assertFalse(RegistrationConflictDetector.isDuplicateKey(
                new SQLException("unavailable", "08000", 0)));
    }
}
