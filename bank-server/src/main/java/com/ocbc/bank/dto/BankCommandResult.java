package com.ocbc.bank.dto;

import com.ocbc.bank.events.UserAccountEvent;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BankCommandResult {
    private final boolean success;
    private final Optional<String> errorMessage;
    private final List<UserAccountEvent> userAccountEvents;

    public BankCommandResult(List<UserAccountEvent> userAccountEvents) {
        this.success = true;
        this.errorMessage = Optional.empty();
        this.userAccountEvents = userAccountEvents;
    }

    public BankCommandResult(String errorMessage, List<UserAccountEvent> userAccountEvents) {
        this.success = false;
        this.errorMessage = Optional.of(errorMessage);
        this.userAccountEvents = userAccountEvents;
    }

    public boolean isSuccess() {
        return success;
    }

    public Optional<String> getErrorMessage() {
        return errorMessage;
    }

    public List<UserAccountEvent> getUserAccountEvents() {
        return userAccountEvents;
    }

    public BankOperationResponse toResponse(String sourceAccount, CommandType commandType) {
        List<BankOperationResult> results = this.userAccountEvents.stream().map(UserAccountEvent::map).collect(Collectors.toList());
        return new BankOperationResponse(sourceAccount, isSuccess(), getErrorMessage().orElse(null), commandType, results);
    }
}
