package com.ocbc.bank.exceptions;

import com.ocbc.bank.command.model.IBankingInstruction;
import com.ocbc.bank.events.UserAccountEvent;

import java.util.Collections;
import java.util.List;

public class BankOperationException extends RuntimeException {
    private IBankingInstruction iBankingInstruction;
    private List<UserAccountEvent> events;

    public BankOperationException(IBankingInstruction iBankingInstruction, List<UserAccountEvent> events, Throwable t) {
        super(t);
        this.iBankingInstruction = iBankingInstruction;
        this.events = Collections.unmodifiableList(events);
    }

    public BankOperationException(IBankingInstruction iBankingInstruction, Throwable t) {
        super(t);
        this.iBankingInstruction = iBankingInstruction;
        this.events = Collections.emptyList();
    }


    public BankOperationException(List<UserAccountEvent> events, Throwable t) {
        super(t);
        this.events = Collections.unmodifiableList(events);
    }

    public List<UserAccountEvent> getEvents() {
        return events;
    }

    @Override
    public String getMessage() {
        return "failed while performing instruction " + (iBankingInstruction != null ? iBankingInstruction.toString() : "-");
    }
}
