package com.ocbc.bank.client.parsers;

import com.ocbc.bank.dto.BankOperationRequest;
import com.ocbc.bank.dto.CommandType;

public class LoginRequestInputParser implements UserInputParser {

    @Override
    public BankOperationRequest getRequest(String loggedInUser, String input) {
        String[] split = input.split("\\s");
        return new BankOperationRequest(split[1], CommandType.LOGIN, null,null);
    }
}
