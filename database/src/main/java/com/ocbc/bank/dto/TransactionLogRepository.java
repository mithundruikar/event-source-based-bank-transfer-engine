package com.ocbc.bank.dto;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TransactionLogRepository extends CrudRepository<TransactionLog, Long> {
    @Query(value = "SELECT * FROM TRANSACTION_LOG where account_id = ?1 ORDER BY create_time", nativeQuery = true)
    List<TransactionLog> getOrderedLogs(String accountId);
}
