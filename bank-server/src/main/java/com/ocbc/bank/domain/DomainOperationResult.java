package com.ocbc.bank.domain;

import com.ocbc.bank.command.model.PayoutDebtsInstruction;
import com.ocbc.bank.events.UserAccountEvent;

import java.util.Collections;
import java.util.Set;

public class DomainOperationResult {
    private final Set<UserAccountEvent> completedEvents;
    private final Set<PayoutDebtsInstruction> payoutDebtsInstructions;

    public DomainOperationResult(Set<UserAccountEvent> completedEvents) {
        this.completedEvents = completedEvents;
        this.payoutDebtsInstructions = Collections.emptySet();
    }

    public DomainOperationResult(Set<UserAccountEvent> completedEvents, Set<PayoutDebtsInstruction> payoutDebtsInstructions) {
        this.completedEvents = completedEvents;
        this.payoutDebtsInstructions = Collections.unmodifiableSet(payoutDebtsInstructions);
    }

    public Set<UserAccountEvent> getCompletedEvents() {
        return completedEvents;
    }

    public Set<PayoutDebtsInstruction> getPayoutDebtsInstructions() {
        return payoutDebtsInstructions;
    }
}
