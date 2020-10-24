package com.ocbc.bank.client;

import lombok.Value;

@Value
public class UserInputMatchResult {
    private final boolean match;
    private final boolean partialMatch;
}
