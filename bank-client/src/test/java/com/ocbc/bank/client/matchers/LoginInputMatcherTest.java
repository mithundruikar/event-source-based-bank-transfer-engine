package com.ocbc.bank.client.matchers;

import org.junit.Test;

import static org.junit.Assert.*;

public class LoginInputMatcherTest {

    @Test
    public void matches() {
        LoginInputMatcher loginInputParser = new LoginInputMatcher();
        assertTrue(loginInputParser.matches("login user1").isMatch());
        assertTrue(loginInputParser.matches("login abcABC").isMatch());
        assertTrue(loginInputParser.matches("login A90001").isMatch());
        assertTrue(loginInputParser.matches("login 3434343").isMatch());
    }

    @Test
    public void invalidMatch() {
        LoginInputMatcher loginInputParser = new LoginInputMatcher();
        assertFalse(loginInputParser.matches("ABC user1").isMatch());
        assertFalse(loginInputParser.matches("login ADS*(@#@").isMatch());
    }

    @Test
    public void partialMatch() {
        LoginInputMatcher loginInputParser = new LoginInputMatcher();
        assertFalse(loginInputParser.matches("loginuser1").isMatch());
        assertTrue(loginInputParser.matches("loginuser1").isPartialMatch());

        assertTrue(loginInputParser.matches("login ADS*(@#@").isPartialMatch());
    }

}