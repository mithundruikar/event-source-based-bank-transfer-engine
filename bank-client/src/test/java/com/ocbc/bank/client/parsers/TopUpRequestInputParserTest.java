package com.ocbc.bank.client.parsers;

import com.ocbc.bank.dto.BankOperationRequest;
import com.ocbc.bank.dto.CommandType;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class TopUpRequestInputParserTest {

    @Test
    public void parse() {
        TopUpRequestInputParser topUpRequestInputParser = new TopUpRequestInputParser();
        BankOperationRequest request = topUpRequestInputParser.getRequest("ABC", "topup 121.21");

        assertEquals("ABC", request.getAccountId());
        assertEquals(new BigDecimal("121.21"), request.getAmount());
        assertEquals(CommandType.TOPUP, request.getCommand());
        assertNull(request.getToAccountId());
    }
}