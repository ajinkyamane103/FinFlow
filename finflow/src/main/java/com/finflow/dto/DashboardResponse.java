package com.finflow.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardResponse(List<BankAccountResponse> accounts,
                                Map<String, BigDecimal> totalBalanceByCurrency,
                                List<TransactionResponse> recentTransactions) {
}
