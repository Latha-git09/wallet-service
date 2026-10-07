package com.latha.wallet.dto;

import java.math.BigDecimal;

public record BalanceResponse(Long userId, BigDecimal balance) {
}
