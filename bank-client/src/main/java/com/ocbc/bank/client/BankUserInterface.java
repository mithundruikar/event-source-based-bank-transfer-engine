package com.ocbc.bank.client;

import com.google.common.annotations.VisibleForTesting;
import com.ocbc.bank.client.matchers.UserInputMatcher;
import com.ocbc.bank.client.port.UserInputPort;
import com.ocbc.bank.client.processor.BankServerInvoker;
import com.ocbc.bank.client.session.UserSession;
import com.ocbc.bank.client.session.UserSessionManager;
import com.ocbc.bank.dto.BankOperationRequest;
import com.ocbc.bank.dto.CommandType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.List;
import java.util.Optional;

public class BankUserInterface implements ApplicationListener<ApplicationReadyEvent> {

    private UserInputPort userInputPort;
    private List<UserInputMatcher> userInputMatchers;
    private UserSessionManager userSessionManager;
    private BankServerInvoker bankServerInvoker;

    public BankUserInterface(UserInputPort userInputPort,
                             List<UserInputMatcher> userInputMatchers,
                             UserSessionManager userSessionManager,
                             BankServerInvoker bankServerInvoker) {
        this.userInputPort = userInputPort;
        this.userInputMatchers = userInputMatchers;
        this.userSessionManager = userSessionManager;
        this.bankServerInvoker = bankServerInvoker;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        start();
    }

    @VisibleForTesting
    protected void start() {
        String input = null;
        printWelcome();
        printUsage();

        while( ! (input = this.userInputPort.getNextLine()).equals("exit")) {
            String currentInput = input;
            if("usage".equals(currentInput)) {
                printUsage();
                continue;
            }
            Optional<UserInputMatcher> any = userInputMatchers
                    .stream()
                    .filter(userInputMatcher -> {
                        UserInputMatchResult matches = userInputMatcher.matches(currentInput);
                        if (matches.isPartialMatch()) {
                            printUsage(userInputMatcher);
                        }
                        return matches.isMatch();
                    }).findAny();

            if(any.isPresent()) {
                UserSession userSession = this.userSessionManager.getUserSession();
                BankOperationRequest request = any.get().getUserInputParser().getRequest(userSession != null ? userSession.getUserAccountId() : null, currentInput);
                if(userSession == null && !CommandType.LOGIN.equals(request.getCommand())) {
                    this.userInputPort.writeLine("please login first before continuing...");
                    printUsage();
                } else {
                    List<String> response = this.bankServerInvoker.getResponse(request);
                    response.forEach(this.userInputPort::writeLine);
                }
            } else {
                this.userInputPort.writeLine("unknown command. type \"usage\" for information");
            }
        }
    }

    private void printUsage() {
        this.userInputPort.writeLine("USAGE guide of supported commands:");
        this.userInputPort.writeLine("------------------------------");
        userInputMatchers.forEach( userInputMatcher -> {
            this.userInputPort.writeLine(userInputMatcher.getUsage());
            this.userInputPort.writeLine(userInputMatcher.getDescription());
            this.userInputPort.writeLine("------------------------------");
        });
        this.userInputPort.writeLine("enter \"exit\" to quite the CLI");
        this.userInputPort.writeLine("------------------------------");
    }

    private void printUsage(UserInputMatcher userInputMatcher) {
        this.userInputPort.writeLine("USAGE guide of supported commands:");
        this.userInputPort.writeLine(userInputMatcher.getUsage());
        this.userInputPort.writeLine(userInputMatcher.getDescription());
        this.userInputPort.writeLine("------------------------------");
        this.userInputPort.writeLine("enter \"exit\" to quite the CLI");
    }

    private void printWelcome() {
        this.userInputPort.writeLine("Welcome to OCBC command line bank account client !!!");
    }
}
