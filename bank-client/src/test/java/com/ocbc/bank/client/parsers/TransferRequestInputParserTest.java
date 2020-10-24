package com.ocbc.bank.client.parsers;

import com.ocbc.bank.dto.BankOperationRequest;
import com.ocbc.bank.dto.CommandType;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class TransferRequestInputParserTest {

    @Test
    public void parse() {
        TransferRequestInputParser transferRequestInputParser = new TransferRequestInputParser();

        BankOperationRequest request = transferRequestInputParser.getRequest("ABC", "pay DEF 121.21");

        assertEquals("ABC", request.getAccountId());
        assertEquals(new BigDecimal("121.21"), request.getAmount());
        assertEquals(CommandType.TRANSFER, request.getCommand());
        assertEquals("DEF", request.getToAccountId());
    }
}