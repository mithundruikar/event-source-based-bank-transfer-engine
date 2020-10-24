package com.ocbc.bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class BankOperationResult {
    private String event;
    private String toAccountId;
    private BigDecimal amount;

    public BankOperationResult(@JsonProperty("event") String event,
                               @JsonProperty(value = "toAccountId", required = false) String toAccountId,
                               @JsonProperty(value = "amount", required = false) BigDecimal amount) {
        this.event = event;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }
}
