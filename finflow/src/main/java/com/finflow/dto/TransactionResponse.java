package com.finflow.dto;

import com.finflow.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(Long id, Long accountId, TransactionType transactionType, BigDecimal amount,
                                  BigDecimal balanceAfter, String description, String transferReference,
                                  LocalDateTime createdAt) {
}
