package com.ocbc.bank.client.matchers;

import com.ocbc.bank.client.UserInputMatchResult;
import com.ocbc.bank.client.parsers.UserInputParser;

import java.util.regex.Pattern;

public interface UserInputMatcher {
    String getCommandKey();
    Pattern getPattern();
    String getUsage();
    String getDescription();
    UserInputParser getUserInputParser();

    default UserInputMatchResult matches(String input) {
        boolean matches = getPattern().matcher(input).matches();
        if(matches) {
            return new UserInputMatchResult(true, false);
        }
        if(input.startsWith(getCommandKey())) {
            return new UserInputMatchResult(false, true);
        }
        return new UserInputMatchResult(false, false);
    }
}

