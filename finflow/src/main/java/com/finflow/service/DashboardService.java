package com.finflow.service;

import com.finflow.dto.DashboardResponse;
import com.finflow.dto.TransactionResponse;
import com.finflow.entity.BankAccount;
import com.finflow.repository.AccountTransactionRepository;
import com.finflow.repository.BankAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

@Service
public class DashboardService {

    private final BankAccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;

    public DashboardService(BankAccountRepository accountRepository,
                            AccountTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String email) {
        var accounts = accountRepository.findByUserEmailOrderByCreatedAtDesc(email);
        var totals = new LinkedHashMap<String, BigDecimal>();
        for (BankAccount account : accounts) {
            totals.merge(account.getCurrency(), account.getBalance(), BigDecimal::add);
        }
        var recent = transactionRepository.findByUserEmailOrderByCreatedAtDesc(email, PageRequest.of(0, 10))
                .map(TransactionService::toResponse).getContent();
        return new DashboardResponse(accounts.stream().map(BankAccountService::toResponse).toList(), totals, recent);
    }
}
