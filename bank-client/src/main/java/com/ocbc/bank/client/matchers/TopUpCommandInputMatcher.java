package com.ocbc.bank.client.matchers;

import com.ocbc.bank.client.parsers.TopUpRequestInputParser;
import com.ocbc.bank.client.parsers.UserInputParser;

import java.util.regex.Pattern;

public class TopUpCommandInputMatcher implements UserInputMatcher {

    public static final String COMMAND_LOGIN_KEY = "topup";
    public static final Pattern PATTERN = Pattern.compile(COMMAND_LOGIN_KEY + " [.0-9]+");

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
        return "topup `<amount>`";
    }

    @Override
    public String getDescription() {
        return "Increase logged-in client balance by `amount`";
    }

    @Override
    public UserInputParser getUserInputParser() {
        return new TopUpRequestInputParser();
    }
}
