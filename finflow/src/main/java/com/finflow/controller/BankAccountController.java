package com.finflow.controller;

import com.finflow.dto.BankAccountResponse;
import com.finflow.dto.CreateBankAccountRequest;
import com.finflow.dto.MoneyTransactionRequest;
import com.finflow.dto.TransactionResponse;
import com.finflow.dto.TransferRequest;
import com.finflow.service.BankAccountService;
import com.finflow.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class BankAccountController {

    private final BankAccountService accountService;
    private final TransactionService transactionService;

    public BankAccountController(BankAccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<BankAccountResponse> create(Authentication authentication,
                                                       @Valid @RequestBody CreateBankAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(authentication.getName(), request));
    }

    @GetMapping
    public List<BankAccountResponse> list(Authentication authentication) {
        return accountService.list(authentication.getName());
    }

    @GetMapping("/{accountId}")
    public BankAccountResponse get(Authentication authentication, @PathVariable Long accountId) {
        return accountService.get(accountId, authentication.getName());
    }

    @PostMapping("/{accountId}/deposits")
    public ResponseEntity<TransactionResponse> deposit(Authentication authentication, @PathVariable Long accountId,
                                                        @Valid @RequestBody MoneyTransactionRequest request,
                                                        @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.deposit(authentication.getName(), accountId, request, key));
    }

    @PostMapping("/{accountId}/withdrawals")
    public ResponseEntity<TransactionResponse> withdraw(Authentication authentication, @PathVariable Long accountId,
                                                         @Valid @RequestBody MoneyTransactionRequest request,
                                                         @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.withdraw(authentication.getName(), accountId, request, key));
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransactionResponse> transfer(Authentication authentication,
                                                        @Valid @RequestBody TransferRequest request,
                                                        @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(authentication.getName(), request, key));
    }

    @GetMapping("/{accountId}/transactions")
    public Page<TransactionResponse> history(Authentication authentication, @PathVariable Long accountId,
                                             @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return transactionService.history(authentication.getName(), accountId, pageable);
    }
}
