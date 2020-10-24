package com.ocbc.bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BankOperationResponse {
    @JsonProperty
    private String accountId;
    @JsonProperty
    private boolean success;
    @JsonProperty(required = false)
    private String errorMessage;
    @JsonProperty
    private CommandType command;
    @JsonProperty
    private List<BankOperationResult> bankOperationResults;

    public BankOperationResponse() {

    }

}
