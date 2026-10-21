package com.latha.wallet.service;

import com.latha.wallet.dto.BalanceResponse;
import com.latha.wallet.dto.PageResponse;
import com.latha.wallet.dto.TransactionResponse;
import com.latha.wallet.dto.TransferRequest;
import com.latha.wallet.entity.AccountStatus;
import com.latha.wallet.entity.TxnStatus;
import com.latha.wallet.entity.TxnType;
import com.latha.wallet.entity.Wallet;
import com.latha.wallet.entity.WalletTransaction;
import com.latha.wallet.exception.AccountFrozenException;
import com.latha.wallet.exception.BadRequestException;
import com.latha.wallet.exception.InsufficientBalanceException;
import com.latha.wallet.exception.ResourceNotFoundException;
import com.latha.wallet.repository.WalletRepository;
import com.latha.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txnRepo;

    private Wallet getWallet(String email) {
        return walletRepo.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for " + email));
    }

    private void checkActive(Wallet wallet, String message) {
        if (wallet.getUser().getStatus() == AccountStatus.FROZEN) {
            throw new AccountFrozenException(message);
        }
    }

    @Transactional
    public BalanceResponse deposit(String email, BigDecimal amount) {
        Wallet wallet = getWallet(email);
        checkActive(wallet, "Your account is frozen");

        wallet.setBalance(wallet.getBalance().add(amount));

        WalletTransaction txn = new WalletTransaction();
        txn.setToWallet(wallet);
        txn.setAmount(amount);
        txn.setType(TxnType.DEPOSIT);
        txn.setStatus(TxnStatus.SUCCESS);
        txnRepo.save(txn);

        return new BalanceResponse(wallet.getUser().getId(), wallet.getBalance());
    }

    @Transactional(readOnly = true)
    public BalanceResponse balance(String email) {
        Wallet wallet = getWallet(email);
        return new BalanceResponse(wallet.getUser().getId(), wallet.getBalance());
    }

    /**
     * Debit sender and credit receiver in ONE transaction: both succeed or both roll back.
     * Wallet has a @Version column, so if two requests change the same wallet at the same
     * time, the second one fails with an optimistic locking error (HTTP 409) instead of
     * silently overwriting the balance (no double spending).
     *
     * Idempotency: if the client sends an Idempotency-Key and we already processed that key,
     * we return the original result and do NOT move money again (safe retries after a timeout).
     * The unique column on idempotencyKey also protects against two identical requests racing.
     */
    @Transactional
    public TransactionResponse transfer(String senderEmail, TransferRequest r, String idempotencyKey) {
        if (senderEmail.equalsIgnoreCase(r.toEmail())) {
            throw new BadRequestException("You cannot transfer money to yourself");
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<WalletTransaction> existing = txnRepo.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                WalletTransaction old = existing.get();
                boolean sameSender = old.getFromWallet() != null
                        && old.getFromWallet().getUser().getEmail().equalsIgnoreCase(senderEmail);
                if (!sameSender) {
                    throw new BadRequestException("Idempotency key already used");
                }
                return toResponse(old);
            }
        }

        Wallet from = getWallet(senderEmail);
        Wallet to = walletRepo.findByUserEmail(r.toEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found: " + r.toEmail()));

        checkActive(from, "Your account is frozen");
        checkActive(to, "Receiver account is frozen");

        if (from.getBalance().compareTo(r.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(r.amount()));
        to.setBalance(to.getBalance().add(r.amount()));
        walletRepo.save(from);
        walletRepo.save(to);

        WalletTransaction txn = new WalletTransaction();
        txn.setFromWallet(from);
        txn.setToWallet(to);
        txn.setAmount(r.amount());
        txn.setType(TxnType.TRANSFER);
        txn.setIdempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey);
        txn.setStatus(TxnStatus.SUCCESS);
        return toResponse(txnRepo.save(txn));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> history(String email, Pageable pageable) {
        Wallet wallet = getWallet(email);
        Page<WalletTransaction> page =
                txnRepo.findByFromWalletIdOrToWalletId(wallet.getId(), wallet.getId(), pageable);

        return new PageResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private TransactionResponse toResponse(WalletTransaction t) {
        String fromEmail = t.getFromWallet() == null ? null : t.getFromWallet().getUser().getEmail();
        String toEmail = t.getToWallet() == null ? null : t.getToWallet().getUser().getEmail();
        return new TransactionResponse(t.getId(), t.getType(), t.getStatus(),
                t.getAmount(), fromEmail, toEmail, t.getCreatedAt());
    }
}
