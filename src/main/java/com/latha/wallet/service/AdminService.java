package com.latha.wallet.service;

import com.latha.wallet.entity.AccountStatus;
import com.latha.wallet.entity.Role;
import com.latha.wallet.entity.User;
import com.latha.wallet.exception.BadRequestException;
import com.latha.wallet.exception.ResourceNotFoundException;
import com.latha.wallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepo;

    @Transactional
    public void setStatus(Long userId, AccountStatus status) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Admin accounts cannot be frozen");
        }
        user.setStatus(status);
        userRepo.save(user);
    }
}
