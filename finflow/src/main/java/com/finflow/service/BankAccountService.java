package com.finflow.service;

import com.finflow.dto.BankAccountResponse;
import com.finflow.dto.CreateBankAccountRequest;
import com.finflow.entity.AccountTransaction;
import com.finflow.entity.BankAccount;
import com.finflow.entity.TransactionType;
import com.finflow.entity.User;
import com.finflow.exception.InvalidRequestException;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.repository.AccountTransactionRepository;
import com.finflow.repository.BankAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

@Service
public class BankAccountService {

    private final BankAccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final UserService userService;

    public BankAccountService(BankAccountRepository accountRepository,
                              AccountTransactionRepository transactionRepository,
                              UserService userService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    @Transactional
    public BankAccountResponse create(String email, CreateBankAccountRequest request) {
        User user = userService.getRequiredUser(email);
        String currency = normalizeCurrency(request.currency());

        BankAccount account = new BankAccount();
        account.setUser(user);
        account.setName(request.name().trim());
        account.setAccountType(request.accountType());
        account.setCurrency(currency);
        account.setBalance(request.initialBalance());
        account = accountRepository.save(account);

        // Initial funding is explicit in the immutable ledger rather than an untracked balance mutation.
        if (request.initialBalance().signum() > 0) {
            AccountTransaction opening = new AccountTransaction();
            opening.setUser(user);
            opening.setAccount(account);
            opening.setTransactionType(TransactionType.OPENING_BALANCE);
            opening.setAmount(request.initialBalance());
            opening.setBalanceAfter(account.getBalance());
            opening.setDescription("Opening balance");
            opening.setIdempotencyKey("opening-" + java.util.UUID.randomUUID());
            transactionRepository.save(opening);
        }
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<BankAccountResponse> list(String email) {
        return accountRepository.findByUserEmailOrderByCreatedAtDesc(email).stream()
                .map(BankAccountService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BankAccountResponse get(Long accountId, String email) {
        return toResponse(getOwnedAccount(accountId, email));
    }

    public BankAccount getOwnedAccount(Long accountId, String email) {
        return accountRepository.findByIdAndUserEmail(accountId, email).orElseThrow(
                () -> new ResourceNotFoundException("Bank account was not found"));
    }

    private String normalizeCurrency(String input) {
        String currency = input.trim().toUpperCase(Locale.ROOT);
        try {
            return Currency.getInstance(currency).getCurrencyCode();
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Currency must be a valid ISO 4217 code");
        }
    }

    public static BankAccountResponse toResponse(BankAccount account) {
        return new BankAccountResponse(account.getId(), account.getName(), account.getAccountType(),
                account.getCurrency(), account.getBalance(), account.getCreatedAt());
    }
}
