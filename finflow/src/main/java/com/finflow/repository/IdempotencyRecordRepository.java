package com.finflow.repository;

import com.finflow.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByUserEmailAndIdempotencyKey(String email, String idempotencyKey);
}
