package com.example.securelogin.auth;

import java.sql.SQLException;

public final class RegistrationConflictDetector {
    private RegistrationConflictDetector() {}

    public static boolean isDuplicateKey(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException
                    && sqlException.getErrorCode() == 1062) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
