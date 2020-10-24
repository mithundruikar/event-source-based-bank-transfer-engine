package com.ocbc.bank.client.printer;

import com.ocbc.bank.dto.BankOperationResponse;
import com.ocbc.bank.dto.BankOperationResult;
import com.ocbc.bank.dto.EventType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class BankOperationResponsePrinter {

    private Map<String, BiFunction<String, BankOperationResult, String>> resultMapper = new HashMap<>();

    public BankOperationResponsePrinter() {
        this.resultMapper.put(EventType.CNF_DEBIT.name(), (acctId, result) -> "debited "+result.getAmount()+ " from "+acctId);
        this.resultMapper.put(EventType.BALANCE_UPDATE.name(), (acctId, result) -> "your balance is "+result.getAmount());
        this.resultMapper.put(EventType.OWING_FROM_UPDATE.name(), (acctId, result) -> "Owing "+result.getAmount()+ " from "+result.getToAccountId());
        this.resultMapper.put(EventType.OWING_TO_UPDATE.name(), (acctId, result) -> "Owing "+result.getAmount()+ " to "+result.getToAccountId());
        this.resultMapper.put(EventType.CNF_PAYBACK_DEBIT.name(), (acctId, result) -> "Transferred "+result.getAmount()+ " to "+result.getToAccountId());
    }
    public List<String> print(BankOperationResponse bankOperationResponse) {
        List<String> response = new ArrayList<>();
        List<String> printableLines = bankOperationResponse.getBankOperationResults().stream()
                .filter(bankOperationResult -> this.resultMapper.containsKey(bankOperationResult.getEvent()))
                .map(bankOperationResult -> this.resultMapper.get(bankOperationResult.getEvent()).apply(bankOperationResponse.getAccountId(), bankOperationResult))
                .collect(Collectors.toList());
        response.addAll(printableLines);
        return response;
    }
}
