package com.ocbc.bank.client.matchers;

import com.ocbc.bank.client.parsers.TopUpRequestInputParser;
import com.ocbc.bank.client.parsers.TransferRequestInputParser;
import com.ocbc.bank.client.parsers.UserInputParser;

import java.util.regex.Pattern;

public class TransferCommandInputMatcher implements UserInputMatcher {

    public static final String COMMAND_LOGIN_KEY = "pay";
    public static final Pattern PATTERN = Pattern.compile(COMMAND_LOGIN_KEY + " [a-zA-Z0-9]+ [.0-9]+");

    @Override
    public String getCommandKey() {
        return COMMAND_LOGIN_KEY;
    }

    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    @Override
    public String getUsage() {
        return "pay `<another_client>` `<amount>`";
    }

    @Override
    public String getDescription() {
        return "Pay `amount` from logged-in client to `another_client`, maybe in parts, as soon as possible";
    }

    @Override
    public UserInputParser getUserInputParser() {
        return new TransferRequestInputParser();
    }
}
