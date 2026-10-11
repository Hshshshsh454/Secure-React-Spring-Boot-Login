package com.example.securelogin.api;

import com.example.securelogin.auth.AccountRateLimiter;
import com.example.securelogin.auth.AuthenticationBackendUnavailableException;
import com.example.securelogin.auth.AuthService;
import com.example.securelogin.auth.EmailNormalizer;
import com.example.securelogin.auth.InvalidCredentialsException;
import com.example.securelogin.auth.PasswordPolicy;
import com.example.securelogin.auth.RegistrationConflictDetector;
import com.example.securelogin.api.dto.AuthMessage;
import com.example.securelogin.api.dto.CsrfResponse;
import com.example.securelogin.api.dto.LoginRequest;
import com.example.securelogin.api.dto.RegisterRequest;
import com.example.securelogin.api.dto.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String GENERIC_REGISTRATION_MESSAGE =
            "Request received. You can now try signing in.";

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final AuthService authService;
    private final AccountRateLimiter accountRateLimiter;
    private final PasswordPolicy passwordPolicy;
    private final int loginLimit;
    private final int loginWindowSeconds;
    private final int registrationLimit;
    private final int registrationWindowSeconds;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            AuthService authService,
            AccountRateLimiter accountRateLimiter,
            PasswordPolicy passwordPolicy,
            @Value("${auth.rate-limit.login-limit:12}") int loginLimit,
            @Value("${auth.rate-limit.login-window-seconds:300}") int loginWindowSeconds,
            @Value("${auth.rate-limit.register-limit:8}") int registrationLimit,
            @Value("${auth.rate-limit.register-window-seconds:3600}") int registrationWindowSeconds) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.authService = authService;
        this.accountRateLimiter = accountRateLimiter;
        this.passwordPolicy = passwordPolicy;
        this.loginLimit = loginLimit;
        this.loginWindowSeconds = loginWindowSeconds;
        this.registrationLimit = registrationLimit;
        this.registrationWindowSeconds = registrationWindowSeconds;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getToken(), csrfToken.getHeaderName());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthMessage> register(@Valid @RequestBody RegisterRequest input) {
        String email = EmailNormalizer.normalize(input.email());
        passwordPolicy.validate(input.password());
        accountRateLimiter.reserve(
                "register",
                email,
                registrationLimit,
                registrationWindowSeconds);

        String passwordHash = authService.hashPassword(input.password());
        try {
            authService.register(email, passwordHash);
        } catch (RuntimeException exception) {
            if (!RegistrationConflictDetector.isDuplicateKey(exception)) {
                throw exception;
            }
            // Registration intentionally does not disclose whether this email
            // already belongs to an account.
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new AuthMessage(GENERIC_REGISTRATION_MESSAGE));
    }

    @PostMapping("/login")
    public UserProfile login(
            @Valid @RequestBody LoginRequest input,
            HttpServletRequest request,
            HttpServletResponse response) {
        String email = EmailNormalizer.normalize(input.email());
        passwordPolicy.validate(input.password());
        accountRateLimiter.reserve("login", email, loginLimit, loginWindowSeconds);

        final Authentication authenticated;
        try {
            authenticated = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, input.password()));
        } catch (InternalAuthenticationServiceException exception) {
            throw new AuthenticationBackendUnavailableException();
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }

        sessionAuthenticationStrategy.onAuthentication(authenticated, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticated);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        accountRateLimiter.clear("login", email);

        return authService.profileFor(email);
    }

    @GetMapping("/me")
    public UserProfile currentUser(
            Authentication authentication,
            HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationServiceException("Authentication required");
        }
        try {
            return authService.profileFor(authentication.getName());
        } catch (AuthenticationServiceException exception) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            throw exception;
        }
    }
}
