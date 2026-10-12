package com.latha.wallet.controller;

import com.latha.wallet.dto.BalanceResponse;
import com.latha.wallet.dto.DepositRequest;
import com.latha.wallet.dto.PageResponse;
import com.latha.wallet.dto.TransactionResponse;
import com.latha.wallet.dto.TransferRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.latha.wallet.service.WalletService;


// The logged-in user comes from the JWT (auth.getName() = email), never from the URL.
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/deposit")
    public BalanceResponse deposit(Authentication auth, @Valid @RequestBody DepositRequest r) {
        return walletService.deposit(auth.getName(), r.amount());
    }

    @GetMapping("/balance")
    public BalanceResponse balance(Authentication auth) {
        return walletService.balance(auth.getName());
    }

    @PostMapping("/transfer")
    public TransactionResponse transfer(Authentication auth, @Valid @RequestBody TransferRequest r) {
        return walletService.transfer(auth.getName(), r);
    }

    @GetMapping("/transactions")
    public PageResponse<TransactionResponse> transactions(
            Authentication auth,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return walletService.history(auth.getName(), pageable);
    }
}
