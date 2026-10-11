package com.example.securelogin.auth;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RateLimitKeyHasher {
    private final byte[] secret;

    public RateLimitKeyHasher(@Value("${auth.rate-limit.hmac-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) {
            throw new IllegalStateException("SESSION_SECRET must contain at least 32 bytes.");
        }
    }

    public byte[] hash(String scope, String normalizedIdentifier) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            mac.update(scope.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) 0);
            return mac.doFinal(normalizedIdentifier.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create an authentication rate-limit key.");
        }
    }
}
