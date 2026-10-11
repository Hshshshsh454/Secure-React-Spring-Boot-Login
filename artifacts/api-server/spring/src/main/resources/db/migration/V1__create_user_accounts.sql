CREATE TABLE user_accounts (
    id VARCHAR(36) NOT NULL,
    email_normalized VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    account_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_user_accounts PRIMARY KEY (id),
    CONSTRAINT uk_user_accounts_email_normalized UNIQUE (email_normalized)
) ENGINE=InnoDB;
