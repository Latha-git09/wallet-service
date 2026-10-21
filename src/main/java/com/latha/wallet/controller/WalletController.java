package com.latha.wallet.controller;

import com.latha.wallet.dto.BalanceResponse;
import com.latha.wallet.dto.DepositRequest;
import com.latha.wallet.dto.PageResponse;
import com.latha.wallet.dto.TransactionResponse;
import com.latha.wallet.dto.TransferRequest;
import com.latha.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The logged-in user comes from the JWT (auth.getName() = email), never from the URL.
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Deposit, transfer, balance and transaction history")
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "Deposit money into your wallet")
    @PostMapping("/deposit")
    public BalanceResponse deposit(Authentication auth, @Valid @RequestBody DepositRequest r) {
        return walletService.deposit(auth.getName(), r.amount());
    }

    @Operation(summary = "Get your current balance")
    @GetMapping("/balance")
    public BalanceResponse balance(Authentication auth) {
        return walletService.balance(auth.getName());
    }

    @Operation(summary = "Transfer money to another user",
            description = "Send an Idempotency-Key header (any unique string, e.g. a UUID). "
                    + "If the same key is sent again, the original result is returned and money is not moved twice.")
    @PostMapping("/transfer")
    public TransactionResponse transfer(
            Authentication auth,
            @Parameter(description = "Unique key per transfer, e.g. a UUID. Makes retries safe.")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest r) {
        return walletService.transfer(auth.getName(), r, idempotencyKey);
    }

    @Operation(summary = "Your transaction history (paginated, newest first)")
    @GetMapping("/transactions")
    public PageResponse<TransactionResponse> transactions(
            Authentication auth,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return walletService.history(auth.getName(), pageable);
    }
}
