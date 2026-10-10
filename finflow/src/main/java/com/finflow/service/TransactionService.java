package com.finflow.service;

import com.finflow.dto.MoneyTransactionRequest;
import com.finflow.dto.TransactionResponse;
import com.finflow.dto.TransferRequest;
import com.finflow.entity.AccountTransaction;
import com.finflow.entity.BankAccount;
import com.finflow.entity.IdempotencyRecord;
import com.finflow.entity.TransactionType;
import com.finflow.entity.User;
import com.finflow.exception.IdempotencyConflictException;
import com.finflow.exception.InsufficientFundsException;
import com.finflow.exception.InvalidRequestException;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.repository.AccountTransactionRepository;
import com.finflow.repository.BankAccountRepository;
import com.finflow.repository.IdempotencyRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
public class TransactionService {

    private final BankAccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final UserService userService;

    public TransactionService(BankAccountRepository accountRepository,
                              AccountTransactionRepository transactionRepository,
                              IdempotencyRecordRepository idempotencyRecordRepository,
                              UserService userService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.userService = userService;
    }

    @Transactional
    public TransactionResponse deposit(String email, Long accountId, MoneyTransactionRequest request,
                                       String idempotencyKey) {
        User user = userService.getRequiredUser(email);
        IdempotencyClaim claim = claim(user, idempotencyKey,
                fingerprint("DEPOSIT", accountId, null, request));
        if (claim.existing()) {
            return existingResponse(email, claim.key(), TransactionType.DEPOSIT);
        }
        BankAccount account = lockedAccount(accountId, email);
        account.setBalance(account.getBalance().add(request.amount()));
        return toResponse(record(user, account, TransactionType.DEPOSIT, request, null, claim.key()));
    }

    @Transactional
    public TransactionResponse withdraw(String email, Long accountId, MoneyTransactionRequest request,
                                        String idempotencyKey) {
        User user = userService.getRequiredUser(email);
        IdempotencyClaim claim = claim(user, idempotencyKey,
                fingerprint("WITHDRAWAL", accountId, null, request));
        if (claim.existing()) {
            return existingResponse(email, claim.key(), TransactionType.WITHDRAWAL);
        }
        BankAccount account = lockedAccount(accountId, email);
        requireSufficientFunds(account, request.amount());
        account.setBalance(account.getBalance().subtract(request.amount()));
        return toResponse(record(user, account, TransactionType.WITHDRAWAL, request, null, claim.key()));
    }

    @Transactional
    public TransactionResponse transfer(String email, TransferRequest request, String idempotencyKey) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new InvalidRequestException("Source and destination accounts must be different");
        }
        User user = userService.getRequiredUser(email);
        MoneyTransactionRequest money = new MoneyTransactionRequest(request.amount(), request.description());
        IdempotencyClaim claim = claim(user, idempotencyKey,
                fingerprint("TRANSFER", request.sourceAccountId(), request.destinationAccountId(), money));
        if (claim.existing()) {
            return existingResponse(email, claim.key(), TransactionType.TRANSFER_OUT);
        }

        // Lock in a stable ID order to avoid two opposite transfers deadlocking each other.
        BankAccount first = lockedAccount(Math.min(request.sourceAccountId(), request.destinationAccountId()), email);
        BankAccount second = lockedAccount(Math.max(request.sourceAccountId(), request.destinationAccountId()), email);
        BankAccount source = request.sourceAccountId().equals(first.getId()) ? first : second;
        BankAccount destination = request.destinationAccountId().equals(first.getId()) ? first : second;

        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new InvalidRequestException("Transfers require accounts with the same currency");
        }
        requireSufficientFunds(source, request.amount());

        source.setBalance(source.getBalance().subtract(request.amount()));
        destination.setBalance(destination.getBalance().add(request.amount()));
        String reference = UUID.randomUUID().toString();
        AccountTransaction debit = record(user, source, TransactionType.TRANSFER_OUT, money, reference, claim.key());
        record(user, destination, TransactionType.TRANSFER_IN, money, reference, claim.key());
        return toResponse(debit);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> history(String email, Long accountId, Pageable pageable) {
        // Explicit ownership lookup prevents querying a ledger for an account the caller does not own.
        if (!accountRepository.findByIdAndUserEmail(accountId, email).isPresent()) {
            throw new ResourceNotFoundException("Bank account was not found");
        }
        return transactionRepository.findByAccountIdAndUserEmail(accountId, email, pageable)
                .map(TransactionService::toResponse);
    }

    private IdempotencyClaim claim(User user, String suppliedKey, String fingerprint) {
        String key = normalizeIdempotencyKey(suppliedKey);
        var existing = idempotencyRecordRepository.findByUserEmailAndIdempotencyKey(user.getEmail(), key);
        if (existing.isPresent()) {
            assertSameRequest(existing.get(), fingerprint);
            return new IdempotencyClaim(key, true);
        }

        IdempotencyRecord record = new IdempotencyRecord();
        record.setUser(user);
        record.setIdempotencyKey(key);
        record.setRequestFingerprint(fingerprint);
        // Flush before a balance is changed. The unique key constraint makes a
        // simultaneous duplicate fail before it can create a second operation.
        // A concurrent constraint violation propagates to the REST exception handler as a 409.
        idempotencyRecordRepository.saveAndFlush(record);
        return new IdempotencyClaim(key, false);
    }

    private TransactionResponse existingResponse(String email, String key, TransactionType type) {
        return transactionRepository.findFirstByUserEmailAndIdempotencyKeyAndTransactionTypeOrderByCreatedAtAsc(
                        email, key, type)
                .map(TransactionService::toResponse)
                .orElseThrow(IdempotencyConflictException::new);
    }

    private static void assertSameRequest(IdempotencyRecord record, String fingerprint) {
        if (!MessageDigest.isEqual(record.getRequestFingerprint().getBytes(StandardCharsets.US_ASCII),
                fingerprint.getBytes(StandardCharsets.US_ASCII))) {
            throw new IdempotencyConflictException();
        }
    }

    private static String fingerprint(String operation, Long accountId, Long destinationAccountId,
                                      MoneyTransactionRequest request) {
        String canonicalRequest = field(operation) + field(String.valueOf(accountId))
                + field(destinationAccountId == null ? "" : String.valueOf(destinationAccountId))
                + field(request.amount().stripTrailingZeros().toPlainString())
                + field(request.description() == null ? "" : request.description().trim());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String field(String value) {
        return value.length() + ":" + value;
    }

    private BankAccount lockedAccount(Long accountId, String email) {
        return accountRepository.findByIdAndUserEmailForUpdate(accountId, email).orElseThrow(
                () -> new ResourceNotFoundException("Bank account was not found"));
    }

    private AccountTransaction record(User user, BankAccount account, TransactionType type,
                                      MoneyTransactionRequest request, String transferReference,
                                      String idempotencyKey) {
        AccountTransaction transaction = new AccountTransaction();
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setTransactionType(type);
        transaction.setAmount(request.amount());
        transaction.setBalanceAfter(account.getBalance());
        transaction.setDescription(request.description() == null ? null : request.description().trim());
        transaction.setTransferReference(transferReference);
        transaction.setIdempotencyKey(idempotencyKey);
        return transactionRepository.save(transaction);
    }

    private static void requireSufficientFunds(BankAccount account, java.math.BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
    }

    private static String normalizeIdempotencyKey(String suppliedKey) {
        String key = suppliedKey == null || suppliedKey.isBlank()
                ? UUID.randomUUID().toString() : suppliedKey.trim();
        if (key.length() > 100) {
            throw new InvalidRequestException("Idempotency-Key must be at most 100 characters");
        }
        return key;
    }

    public static TransactionResponse toResponse(AccountTransaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getAccount().getId(),
                transaction.getTransactionType(), transaction.getAmount(), transaction.getBalanceAfter(),
                transaction.getDescription(), transaction.getTransferReference(), transaction.getCreatedAt());
    }

    private record IdempotencyClaim(String key, boolean existing) { }
}
