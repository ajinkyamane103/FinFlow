package com.finflow.repository;

import com.finflow.entity.AccountTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    Page<AccountTransaction> findByAccountIdAndUserEmail(Long accountId, String email, Pageable pageable);

    Page<AccountTransaction> findByUserEmailOrderByCreatedAtDesc(String email, Pageable pageable);

    Optional<AccountTransaction> findFirstByUserEmailAndIdempotencyKeyOrderByCreatedAtAsc(
            String email, String idempotencyKey);

    Optional<AccountTransaction> findFirstByUserEmailAndIdempotencyKeyAndTransactionTypeOrderByCreatedAtAsc(
            String email, String idempotencyKey, com.finflow.entity.TransactionType transactionType);
}
