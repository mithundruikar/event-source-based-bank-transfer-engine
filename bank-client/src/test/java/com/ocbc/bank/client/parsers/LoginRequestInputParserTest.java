package com.ocbc.bank.client.parsers;

import com.ocbc.bank.dto.BankOperationRequest;
import com.ocbc.bank.dto.CommandType;
import org.junit.Test;


import static org.junit.Assert.*;

public class LoginRequestInputParserTest {

    @Test
    public void parse() {
        LoginRequestInputParser loginRequestInputParser = new LoginRequestInputParser();
        BankOperationRequest request = loginRequestInputParser.getRequest("login ABC");

        assertEquals("ABC", request.getAccountId());
        assertNull(request.getAmount());
        assertEquals(CommandType.LOGIN, request.getCommand());
        assertNull(request.getToAccountId());
    }
}