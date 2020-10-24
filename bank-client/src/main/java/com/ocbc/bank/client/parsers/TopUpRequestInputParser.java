package com.ocbc.bank.client.parsers;

import com.ocbc.bank.dto.BankOperationRequest;
import com.ocbc.bank.dto.CommandType;

import java.math.BigDecimal;

public class TopUpRequestInputParser implements UserInputParser {

    @Override
    public BankOperationRequest getRequest(String loggedInUserAccount, String input) {
        String[] split = input.split("\\s");
        return new BankOperationRequest(loggedInUserAccount, CommandType.TOPUP, null,
                new BigDecimal(split[1]));
    }
}
