package com.latha.wallet.dto;

public record LoginResponse(String token, String tokenType, String email, String role) {
}
