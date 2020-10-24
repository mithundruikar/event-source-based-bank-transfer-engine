package com.ocbc.bank.client.matchers;

import org.junit.Test;

import static org.junit.Assert.*;

public class TransferCommandInputMatcherTest {
    @Test
    public void matches() {
        TransferCommandInputMatcher transferCommandInputParser = new TransferCommandInputMatcher();
        assertTrue(transferCommandInputParser.matches("pay user1 1221").isMatch());
        assertTrue(transferCommandInputParser.matches("pay user2 1212.43").isMatch());
        assertTrue(transferCommandInputParser.matches("pay s323232 1000.232").isMatch());
    }

    @Test
    public void invalidMatch() {
        TransferCommandInputMatcher transferCommandInputParser = new TransferCommandInputMatcher();
        assertFalse(transferCommandInputParser.matches("ABC user1").isMatch());
        assertFalse(transferCommandInputParser.matches("FGFG ADS*(@#@ 123").isMatch());
    }

    @Test
    public void partialMatch() {
        TransferCommandInputMatcher transferCommandInputParser = new TransferCommandInputMatcher();
        assertFalse(transferCommandInputParser.matches("paysdsaas").isMatch());
        assertTrue(transferCommandInputParser.matches("pay dsds fdds").isPartialMatch());
        assertTrue(transferCommandInputParser.matches("pay %#$%$fgf").isPartialMatch());
    }
}