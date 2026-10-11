package com.example.securelogin.auth;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AccountRateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(AccountRateLimiter.class);

    private static final String RESERVE_ATTEMPT_SQL = """
            INSERT INTO auth_rate_limits (key_hash, window_started_epoch, attempt_count)
            VALUES (?, FLOOR(UNIX_TIMESTAMP(UTC_TIMESTAMP(6))), 1)
            ON DUPLICATE KEY UPDATE
              attempt_count = IF(
                window_started_epoch <= FLOOR(UNIX_TIMESTAMP(UTC_TIMESTAMP(6))) - ?,
                1,
                attempt_count + 1
              ),
              window_started_epoch = IF(
                window_started_epoch <= FLOOR(UNIX_TIMESTAMP(UTC_TIMESTAMP(6))) - ?,
                FLOOR(UNIX_TIMESTAMP(UTC_TIMESTAMP(6))),
                window_started_epoch
              )
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RateLimitKeyHasher keyHasher;
    private final int cleanupDays;

    public AccountRateLimiter(
            JdbcTemplate jdbcTemplate,
            RateLimitKeyHasher keyHasher,
            @Value("${auth.rate-limit.cleanup-days:1}") int cleanupDays) {
        this.jdbcTemplate = jdbcTemplate;
        this.keyHasher = keyHasher;
        this.cleanupDays = cleanupDays;
    }

    public void reserve(String scope, String normalizedIdentifier, int limit, int windowSeconds) {
        byte[] key = keyHasher.hash(scope, normalizedIdentifier);
        jdbcTemplate.update(RESERVE_ATTEMPT_SQL, key, windowSeconds, windowSeconds);

        Map<String, Object> state = jdbcTemplate.queryForMap("""
                SELECT attempt_count, window_started_epoch,
                       FLOOR(UNIX_TIMESTAMP(UTC_TIMESTAMP(6))) AS now_epoch
                FROM auth_rate_limits
                WHERE key_hash = ?
                """, key);

        int attempts = ((Number) state.get("attempt_count")).intValue();
        if (attempts > limit) {
            long windowStart = ((Number) state.get("window_started_epoch")).longValue();
            long now = ((Number) state.get("now_epoch")).longValue();
            throw new RateLimitExceededException(
                    Math.toIntExact(Math.max(1, windowStart + windowSeconds - now)));
        }
    }

    public void clear(String scope, String normalizedIdentifier) {
        jdbcTemplate.update(
                "DELETE FROM auth_rate_limits WHERE key_hash = ?",
                keyHasher.hash(scope, normalizedIdentifier));
    }

    @Scheduled(
            fixedDelayString = "${auth.rate-limit.cleanup-interval-ms:3600000}",
            initialDelayString = "${auth.rate-limit.cleanup-initial-delay-ms:3600000}")
    public void removeExpiredRateLimitKeys() {
        try {
            jdbcTemplate.update("""
                    DELETE FROM auth_rate_limits
                    WHERE window_started_epoch <
                      FLOOR(UNIX_TIMESTAMP(UTC_TIMESTAMP(6))) - (? * 86400)
                    """, cleanupDays);
        } catch (RuntimeException exception) {
            logger.warn("Authentication rate-limit cleanup failed ({})",
                    exception.getClass().getSimpleName());
        }
    }
}
