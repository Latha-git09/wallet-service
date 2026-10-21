package com.latha.wallet.repository;

import com.latha.wallet.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    // all transactions where this wallet is the sender or the receiver
    Page<WalletTransaction> findByFromWalletIdOrToWalletId(Long fromWalletId, Long toWalletId, Pageable pageable);

    // Day 3: used for idempotency (same key = same request, do not process twice)
    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);
}
