package com.apexledger.core_banking.service;

import com.apexledger.core_banking.dto.AuthResponse;
import com.apexledger.core_banking.dto.RegisterRequest;
import com.apexledger.core_banking.model.Account;
import com.apexledger.core_banking.model.AppUser;
import com.apexledger.core_banking.repository.AccountRepository;
import com.apexledger.core_banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountRepository accountRepository;

    public AuthResponse register(RegisterRequest registerRequest) {
        var user = AppUser.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role("USER")
                .build();

        userRepository.save(user);

        String randomAccountNumber = String.valueOf(1000000000L + new Random().nextInt(900000000));

        Account defaultAccount = new Account();
        defaultAccount.setAccountNumber("ACC-" + randomAccountNumber);
        defaultAccount.setCurrency("INR");
        defaultAccount.setStatus("Active");

        defaultAccount.setUser(user);

        accountRepository.save(defaultAccount);

        var jwtToken =  jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }
}
