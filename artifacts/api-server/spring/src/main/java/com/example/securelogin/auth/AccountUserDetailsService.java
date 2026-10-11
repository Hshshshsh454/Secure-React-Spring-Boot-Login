package com.example.securelogin.auth;

import com.example.securelogin.account.UserAccount;
import com.example.securelogin.account.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountUserDetailsService implements UserDetailsService {
    private final UserAccountRepository userAccountRepository;

    public AccountUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository
                .findByEmailNormalized(EmailNormalizer.normalize(email))
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));

        return User.withUsername(account.getEmailNormalized())
                .password(account.getPasswordHash())
                .disabled(!account.isEnabled())
                .authorities("ROLE_USER")
                .build();
    }
}
