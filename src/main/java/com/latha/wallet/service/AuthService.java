package com.latha.wallet.service;

import com.latha.wallet.dto.RegisterRequest;
import com.latha.wallet.dto.UserResponse;
import com.latha.wallet.entity.User;
import com.latha.wallet.entity.Wallet;
import com.latha.wallet.exception.DuplicateResourceException;
import com.latha.wallet.repository.UserRepository;
import com.latha.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final PasswordEncoder encoder;

    @Transactional
    public UserResponse register(RegisterRequest r) {
        if (userRepo.existsByEmail(r.email())) {
            throw new DuplicateResourceException("Email already registered");
        }
        User user = new User();
        user.setName(r.name());
        user.setEmail(r.email());
        user.setPassword(encoder.encode(r.password()));
        user = userRepo.save(user);

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        walletRepo.save(wallet);

        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
