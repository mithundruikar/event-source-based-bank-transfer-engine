package com.ocbc.bank.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@EqualsAndHashCode
public class TransactionLog {
    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator = "TRANSACTION_LOG_SEQUENCE")
    @SequenceGenerator(name = "TRANSACTION_LOG_SEQUENCE", sequenceName = "TRANSACTION_LOG_SEQUENCE", allocationSize = 1)
    @EqualsAndHashCode.Exclude
    private Long Id;
    private String accountId;
    private String toAccountId;
    private String transactionType;
    private BigDecimal amount;
    private String direction;
    private LocalDateTime createTime;

    public TransactionLog() {

    }

    public TransactionLog(String accountId, String toAccountId, String transactionType, BigDecimal amount, String direction, LocalDateTime createTime) {
        this.accountId = accountId;
        this.toAccountId = toAccountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.direction = direction;
        this.createTime = createTime;
    }
}
