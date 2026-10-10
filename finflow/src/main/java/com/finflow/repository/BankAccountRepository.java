package com.finflow.repository;

import com.finflow.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByUserEmailOrderByCreatedAtDesc(String email);

    Optional<BankAccount> findByIdAndUserEmail(Long id, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from BankAccount account where account.id = :id and account.user.email = :email")
    Optional<BankAccount> findByIdAndUserEmailForUpdate(Long id, String email);
}
