package com.ocbc.bank.client.matchers;

import com.ocbc.bank.client.parsers.LoginRequestInputParser;
import com.ocbc.bank.client.parsers.UserInputParser;

import java.util.regex.Pattern;

public class LoginInputMatcher implements UserInputMatcher {

    public static final String COMMAND_LOGIN_KEY = "login";
    public static final Pattern PATTERN = Pattern.compile(COMMAND_LOGIN_KEY + " [a-zA-Z0-9]+");

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
        return "login `<client>`";
    }

    @Override
    public String getDescription() {
        return "Login as `client`. Creates a new client if not yet exists.";
    }

    @Override
    public UserInputParser getUserInputParser() {
        return new LoginRequestInputParser();
    }
}
