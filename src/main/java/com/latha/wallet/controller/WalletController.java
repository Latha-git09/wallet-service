package com.latha.wallet.controller;

import com.latha.wallet.dto.BalanceResponse;
import com.latha.wallet.dto.DepositRequest;
import com.latha.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    // {userId} in the path is only for Day 1. On Day 2, JWT identifies the logged-in user.
    @PostMapping("/{userId}/deposit")
    public BalanceResponse deposit(@PathVariable Long userId,
                                   @Valid @RequestBody DepositRequest r) {
        return walletService.deposit(userId, r.amount());
    }

    @GetMapping("/{userId}/balance")
    public BalanceResponse balance(@PathVariable Long userId) {
        return walletService.balance(userId);
    }
}
