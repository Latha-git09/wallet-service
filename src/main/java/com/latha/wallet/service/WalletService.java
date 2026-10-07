package com.latha.wallet.service;

import com.latha.wallet.dto.BalanceResponse;
import com.latha.wallet.entity.AccountStatus;
import com.latha.wallet.entity.TxnStatus;
import com.latha.wallet.entity.TxnType;
import com.latha.wallet.entity.Wallet;
import com.latha.wallet.entity.WalletTransaction;
import com.latha.wallet.exception.AccountFrozenException;
import com.latha.wallet.exception.ResourceNotFoundException;
import com.latha.wallet.repository.WalletRepository;
import com.latha.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txnRepo;

    private Wallet getWallet(Long userId) {
        return walletRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user " + userId));
    }

    @Transactional
    public BalanceResponse deposit(Long userId, BigDecimal amount) {
        Wallet wallet = getWallet(userId);
        if (wallet.getUser().getStatus() == AccountStatus.FROZEN) {
            throw new AccountFrozenException("Account is frozen");
        }
        wallet.setBalance(wallet.getBalance().add(amount));

        WalletTransaction txn = new WalletTransaction();
        txn.setToWallet(wallet);
        txn.setAmount(amount);
        txn.setType(TxnType.DEPOSIT);
        txn.setStatus(TxnStatus.SUCCESS);
        txnRepo.save(txn);

        return new BalanceResponse(userId, wallet.getBalance());
    }

    @Transactional(readOnly = true)
    public BalanceResponse balance(Long userId) {
        return new BalanceResponse(userId, getWallet(userId).getBalance());
    }
}
