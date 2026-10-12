package com.latha.wallet.repository;

import com.latha.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);

    Optional<Wallet> findByUserEmail(String email);
}
