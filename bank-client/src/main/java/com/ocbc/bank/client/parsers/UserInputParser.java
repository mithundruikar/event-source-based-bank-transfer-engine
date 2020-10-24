package com.ocbc.bank.client.parsers;

import com.ocbc.bank.dto.BankOperationRequest;

public interface UserInputParser {
    default BankOperationRequest getRequest(String input) {
        return getRequest(null, input);
    };
    BankOperationRequest getRequest(String loggedInUser, String input);
}
