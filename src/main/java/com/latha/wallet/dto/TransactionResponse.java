package com.latha.wallet.dto;

import com.latha.wallet.entity.TxnStatus;
import com.latha.wallet.entity.TxnType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        TxnType type,
        TxnStatus status,
        BigDecimal amount,
        String fromEmail,
        String toEmail,
        LocalDateTime createdAt) {
}
