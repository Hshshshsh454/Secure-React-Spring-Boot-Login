CREATE TABLE auth_rate_limits (
    key_hash BINARY(32) NOT NULL,
    window_started_epoch BIGINT NOT NULL,
    attempt_count INT NOT NULL,
    CONSTRAINT pk_auth_rate_limits PRIMARY KEY (key_hash),
    INDEX ix_auth_rate_limits_window (window_started_epoch)
) ENGINE=InnoDB;
