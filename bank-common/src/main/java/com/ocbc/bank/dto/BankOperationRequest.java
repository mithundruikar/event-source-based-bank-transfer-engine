package com.ocbc.bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@ToString
public class BankOperationRequest {
    private String accountId;
    private CommandType command;
    private String toAccountId;
    private BigDecimal amount;

    public BankOperationRequest(@JsonProperty("accountId") String accountId,
                                @JsonProperty("requestType") CommandType command,
                                @JsonProperty(value = "toAccountId", required = false) String toAccountId,
                                @JsonProperty(value = "amount", required = false) BigDecimal amount) {
        this.accountId = accountId;
        this.command = command;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }
}
