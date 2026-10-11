package com.example.securelogin.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_accounts")
public class UserAccount {
    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "email_normalized", length = 254, nullable = false, unique = true)
    private String emailNormalized;

    @Column(name = "password_hash", length = 100, nullable = false)
    private String passwordHash;

    @Column(name = "account_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserAccount() {}

    public UserAccount(String id, String emailNormalized, String passwordHash, Instant createdAt) {
        this.id = id;
        this.emailNormalized = emailNormalized;
        this.passwordHash = passwordHash;
        this.enabled = true;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getEmailNormalized() {
        return emailNormalized;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
