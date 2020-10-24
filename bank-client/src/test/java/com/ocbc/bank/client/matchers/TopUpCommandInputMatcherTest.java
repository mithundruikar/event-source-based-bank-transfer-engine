package com.ocbc.bank.client.matchers;

import org.junit.Test;

import static org.junit.Assert.*;

public class TopUpCommandInputMatcherTest {
    @Test
    public void matches() {
        TopUpCommandInputMatcher topUpCommandInputParser = new TopUpCommandInputMatcher();
        assertTrue(topUpCommandInputParser.matches("topup 1221").isMatch());
        assertTrue(topUpCommandInputParser.matches("topup 1212.43").isMatch());
        assertTrue(topUpCommandInputParser.matches("topup 1000.232").isMatch());
    }

    @Test
    public void invalidMatch() {
        TopUpCommandInputMatcher topUpCommandInputParser = new TopUpCommandInputMatcher();
        assertFalse(topUpCommandInputParser.matches("ABC user1").isMatch());
        assertFalse(topUpCommandInputParser.matches("FGFG ADS*(@#@ 123").isMatch());
    }

    @Test
    public void partialMatch() {
        TopUpCommandInputMatcher topUpCommandInputParser = new TopUpCommandInputMatcher();
        assertFalse(topUpCommandInputParser.matches("topupsdsaas").isMatch());
        assertTrue(topUpCommandInputParser.matches("topup dsds fdds").isPartialMatch());
        assertTrue(topUpCommandInputParser.matches("topup %#$%$fgf").isPartialMatch());
    }
}