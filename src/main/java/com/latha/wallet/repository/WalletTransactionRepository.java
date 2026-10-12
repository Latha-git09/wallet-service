package com.latha.wallet.repository;

import com.latha.wallet.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    // all transactions where this wallet is the sender or the receiver
    Page<WalletTransaction> findByFromWalletIdOrToWalletId(Long fromWalletId, Long toWalletId, Pageable pageable);
}
