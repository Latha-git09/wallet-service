package com.latha.wallet.service;

import com.latha.wallet.dto.TransactionResponse;
import com.latha.wallet.dto.TransferRequest;
import com.latha.wallet.entity.AccountStatus;
import com.latha.wallet.entity.TxnStatus;
import com.latha.wallet.entity.TxnType;
import com.latha.wallet.entity.User;
import com.latha.wallet.entity.Wallet;
import com.latha.wallet.entity.WalletTransaction;
import com.latha.wallet.exception.AccountFrozenException;
import com.latha.wallet.exception.BadRequestException;
import com.latha.wallet.exception.InsufficientBalanceException;
import com.latha.wallet.repository.WalletRepository;
import com.latha.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock WalletRepository walletRepo;
    @Mock WalletTransactionRepository txnRepo;
    @InjectMocks WalletService service;

    Wallet sender;
    Wallet receiver;

    private Wallet newWallet(Long id, String email, String balance) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        Wallet w = new Wallet();
        w.setId(id);
        w.setUser(u);
        w.setBalance(new BigDecimal(balance));
        return w;
    }

    @BeforeEach
    void setUp() {
        sender = newWallet(1L, "a@x.com", "100.00");
        receiver = newWallet(2L, "b@x.com", "50.00");
    }

    @Test
    void deposit_increasesBalance() {
        when(walletRepo.findByUserEmail("a@x.com")).thenReturn(Optional.of(sender));

        service.deposit("a@x.com", new BigDecimal("25.00"));

        assertThat(sender.getBalance()).isEqualByComparingTo("125.00");
        verify(txnRepo).save(any(WalletTransaction.class));
    }

    @Test
    void transfer_movesMoneyFromSenderToReceiver() {
        when(walletRepo.findByUserEmail("a@x.com")).thenReturn(Optional.of(sender));
        when(walletRepo.findByUserEmail("b@x.com")).thenReturn(Optional.of(receiver));
        when(txnRepo.save(any(WalletTransaction.class))).thenAnswer(i -> i.getArgument(0));

        TransactionResponse res = service.transfer("a@x.com",
                new TransferRequest("b@x.com", new BigDecimal("30.00")), null);

        assertThat(sender.getBalance()).isEqualByComparingTo("70.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("80.00");
        assertThat(res.type()).isEqualTo(TxnType.TRANSFER);
        assertThat(res.status()).isEqualTo(TxnStatus.SUCCESS);
    }

    @Test
    void transfer_failsWhenBalanceIsLow_andChangesNothing() {
        when(walletRepo.findByUserEmail("a@x.com")).thenReturn(Optional.of(sender));
        when(walletRepo.findByUserEmail("b@x.com")).thenReturn(Optional.of(receiver));

        assertThatThrownBy(() -> service.transfer("a@x.com",
                new TransferRequest("b@x.com", new BigDecimal("500.00")), null))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("50.00");
        verify(txnRepo, never()).save(any());
    }

    @Test
    void transfer_toYourself_isRejected() {
        assertThatThrownBy(() -> service.transfer("a@x.com",
                new TransferRequest("a@x.com", new BigDecimal("10.00")), null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void transfer_fromFrozenAccount_isRejected() {
        sender.getUser().setStatus(AccountStatus.FROZEN);
        when(walletRepo.findByUserEmail("a@x.com")).thenReturn(Optional.of(sender));
        when(walletRepo.findByUserEmail("b@x.com")).thenReturn(Optional.of(receiver));

        assertThatThrownBy(() -> service.transfer("a@x.com",
                new TransferRequest("b@x.com", new BigDecimal("10.00")), null))
                .isInstanceOf(AccountFrozenException.class);

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void transfer_withNewIdempotencyKey_savesTheKey() {
        when(txnRepo.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(walletRepo.findByUserEmail("a@x.com")).thenReturn(Optional.of(sender));
        when(walletRepo.findByUserEmail("b@x.com")).thenReturn(Optional.of(receiver));
        when(txnRepo.save(any(WalletTransaction.class))).thenAnswer(i -> i.getArgument(0));

        service.transfer("a@x.com", new TransferRequest("b@x.com", new BigDecimal("10.00")), "key-1");

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(txnRepo).save(captor.capture());
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("key-1");
    }

    @Test
    void transfer_withRepeatedIdempotencyKey_returnsOldResult_andDoesNotMoveMoneyAgain() {
        WalletTransaction old = new WalletTransaction();
        old.setFromWallet(sender);
        old.setToWallet(receiver);
        old.setAmount(new BigDecimal("10.00"));
        old.setType(TxnType.TRANSFER);
        old.setStatus(TxnStatus.SUCCESS);
        old.setIdempotencyKey("key-1");
        when(txnRepo.findByIdempotencyKey("key-1")).thenReturn(Optional.of(old));

        TransactionResponse res = service.transfer("a@x.com",
                new TransferRequest("b@x.com", new BigDecimal("10.00")), "key-1");

        assertThat(res.amount()).isEqualByComparingTo("10.00");
        assertThat(sender.getBalance()).isEqualByComparingTo("100.00"); // unchanged
        verify(txnRepo, never()).save(any());
    }

    @Test
    void transfer_idempotencyKeyUsedByAnotherUser_isRejected() {
        WalletTransaction old = new WalletTransaction();
        old.setFromWallet(receiver); // key was created by b@x.com
        old.setToWallet(sender);
        old.setAmount(new BigDecimal("10.00"));
        when(txnRepo.findByIdempotencyKey("key-1")).thenReturn(Optional.of(old));

        assertThatThrownBy(() -> service.transfer("a@x.com",
                new TransferRequest("b@x.com", new BigDecimal("10.00")), "key-1"))
                .isInstanceOf(BadRequestException.class);
    }
}
