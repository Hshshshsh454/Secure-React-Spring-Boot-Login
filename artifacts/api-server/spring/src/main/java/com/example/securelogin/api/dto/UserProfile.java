package com.example.securelogin.api.dto;

import com.example.securelogin.account.UserAccount;
import java.time.Instant;

public record UserProfile(String id, String email, Instant createdAt) {
    public static UserProfile from(UserAccount account) {
        return new UserProfile(account.getId(), account.getEmailNormalized(), account.getCreatedAt());
    }
}
