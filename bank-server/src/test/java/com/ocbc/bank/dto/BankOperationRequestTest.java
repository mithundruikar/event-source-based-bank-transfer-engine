package com.ocbc.bank.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class BankOperationRequestTest {


    @Test
    public void loginRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String json = String.format("{\"accountId\": \"A\", \"requestType\": \"LOGIN\"}");
        BankOperationRequest bankOperationRequest = objectMapper.readValue(json, BankOperationRequest.class);
        assertEquals("A", bankOperationRequest.getAccountId());
        assertEquals(CommandType.LOGIN, bankOperationRequest.getCommand());
    }

    @Test
    public void transferRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String json = String.format("{'accountId': 'A', 'requestType': 'TRANSFER', 'toAccountId': 'B', 'amount': '12121.12' }");
        json = json.replaceAll("\'", "\"");
        BankOperationRequest bankOperationRequest = objectMapper.readValue(json, BankOperationRequest.class);
        assertEquals("A", bankOperationRequest.getAccountId());
        assertEquals(CommandType.TRANSFER, bankOperationRequest.getCommand());
        assertEquals("B", bankOperationRequest.getToAccountId());
        assertEquals(new BigDecimal("12121.12"), bankOperationRequest.getAmount());
    }


    @Test
    public void topUpRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String json = String.format("{'accountId': 'A', 'requestType': 'TOPUP', 'amount': '12121.12' }");
        json = json.replaceAll("\'", "\"");
        BankOperationRequest bankOperationRequest = objectMapper.readValue(json, BankOperationRequest.class);
        assertEquals("A", bankOperationRequest.getAccountId());
        assertEquals(CommandType.TOPUP, bankOperationRequest.getCommand());
        assertEquals(new BigDecimal("12121.12"), bankOperationRequest.getAmount());
    }

    @Test(expected = InvalidFormatException.class)
    public void invalidRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String json = String.format("{'accountId': 'A', 'requestType': 'UNKNOWNREQUEST', 'amount': '12121.12' }");
        json = json.replaceAll("\'", "\"");
        objectMapper.readValue(json, BankOperationRequest.class);
    }

}