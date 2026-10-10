package com.finflow.dto;

import com.finflow.entity.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankAccountResponse(Long id, String name, AccountType accountType, String currency,
                                  BigDecimal balance, LocalDateTime createdAt) {
}
