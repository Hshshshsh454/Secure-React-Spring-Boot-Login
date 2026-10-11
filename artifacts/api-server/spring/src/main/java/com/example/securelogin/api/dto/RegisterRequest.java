package com.example.securelogin.api.dto;

import com.example.securelogin.auth.EmailNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotNull @Email @Size(max = 254) String email,
        @NotNull @Size(min = 12, max = 72) String password) {
    public RegisterRequest {
        email = EmailNormalizer.normalize(email);
    }
}
