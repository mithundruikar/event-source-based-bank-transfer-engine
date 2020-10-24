package com.ocbc.bank.command.dispatcher;

import com.ocbc.bank.command.model.PayoutDebtsInstruction;
import com.ocbc.bank.domain.DomainOperationResult;
import com.ocbc.bank.domain.UserAccount;
import com.ocbc.bank.domain.UserAccountRepository;
import com.ocbc.bank.events.*;
import com.ocbc.bank.exceptions.BankOperationException;
import com.ocbc.bank.exceptions.UserAccountInvalidStateException;

import java.util.*;
import java.util.stream.Collectors;

public class CreditSideTransferEventsHandler {
    private UserAccountRepository userAccountRepository;
    private AsyncEventDispatcher asyncEventDispatcher;

    public CreditSideTransferEventsHandler(UserAccountRepository userAccountRepository,
                                           AsyncEventDispatcher asyncEventDispatcher) {
        this.userAccountRepository = userAccountRepository;
        this.asyncEventDispatcher = asyncEventDispatcher;
    }

    public Set<UserAccountEvent> handleCreditEvents(String toAccountId, Set<UserAccountEvent> transferEvents) {
        UserAccount toAccountBean = userAccountRepository.getUserAccount(toAccountId);

        Set<UserAccountEvent> producedEvents = new LinkedHashSet<>();
        Set<UserAccountEvent> otherPaybackCreditEvents = new LinkedHashSet<>();
        Set<String> accountIdsToRecoverOnFailure = new HashSet<>();
        toAccountBean.getReentrantLock().lock();
        try {
            Optional<ActualCreditedEvent> actualCreditedEventOptional = UserAccount.filterOptionalEvent(transferEvents, ActualCreditedEvent.class);
            if(actualCreditedEventOptional.isPresent()) {
                ActualCreditedEvent actualCreditedEvent = actualCreditedEventOptional.get();
                producedEvents.add(actualCreditedEvent);
                DomainOperationResult domainOperationResult = toAccountBean.handleAccountCreditedEvent(actualCreditedEvent);
                producedEvents.addAll(domainOperationResult.getCompletedEvents());

                handlePayoutDebtsInstructionsForAccount(toAccountBean, domainOperationResult.getPayoutDebtsInstructions(), producedEvents, otherPaybackCreditEvents, accountIdsToRecoverOnFailure);
            }

            Optional<PaybackCreditEvent> paybackCreditEventOptional = UserAccount.filterOptionalEvent(transferEvents, PaybackCreditEvent.class);
            if(paybackCreditEventOptional.isPresent()) {
                PaybackCreditEvent paybackCreditEvent = paybackCreditEventOptional.get();
                producedEvents.add(paybackCreditEvent);
                producedEvents.addAll(toAccountBean.handlePaybackCreditEvent(paybackCreditEvent));
            }

            Optional<PendingCreditedEvent> pendingCreditedEvent = UserAccount.filterOptionalEvent(transferEvents, PendingCreditedEvent.class);
            if(pendingCreditedEvent.isPresent()) {
                producedEvents.add(pendingCreditedEvent.get());
                producedEvents.addAll(toAccountBean.handlePendingCreditedEvent(pendingCreditedEvent.get()));
            }
        } catch(Throwable throwable) {
            toAccountBean.forceRecovery();
            accountIdsToRecoverOnFailure.stream().map(userAccountRepository::getIfPresent).filter(Objects::nonNull)
                .forEach(UserAccount::forceRecovery);
            throw new BankOperationException(new ArrayList(producedEvents), throwable);
        } finally {
            toAccountBean.getReentrantLock().unlock();
        }
        Map<String, List<UserAccountEvent>> collect = otherPaybackCreditEvents.stream().collect(Collectors.groupingBy(UserAccountEvent::getAccountId));
        collect.entrySet().forEach( entry ->
            this.asyncEventDispatcher.dispatch(() ->
                this.handleCreditEvents(entry.getKey(), new LinkedHashSet(entry.getValue()))
            )
        );
        return producedEvents;
    }

    private void handlePayoutDebtsInstructionsForAccount(UserAccount toAccountBean,
                                                         Set<PayoutDebtsInstruction> payoutDebtsInstructions,
                                                         Set<UserAccountEvent> producedEvents,
                                                         Set<UserAccountEvent> otherPaybackCreditEvents,
                                                         Set<String> accountIdsToRecoverOnFailure) throws UserAccountInvalidStateException {
        for (PayoutDebtsInstruction payoutDebtsInstruction : payoutDebtsInstructions) {
            UserAccount userAccount = this.userAccountRepository.getUserAccount(payoutDebtsInstruction.getToAccountId());
            accountIdsToRecoverOnFailure.add(userAccount.getAccountId());

            Set<UserAccountEvent> payback = toAccountBean.payback(payoutDebtsInstruction);

            PaybackDebitEvent paybackDebitEvent = UserAccount.filterEvent(payback, PaybackDebitEvent.class);
            producedEvents.add(paybackDebitEvent);
            producedEvents.addAll(toAccountBean.handlePaybackDebitEvent(paybackDebitEvent));

            ActualDebitedEvent actualDebitedEvent = UserAccount.filterEvent(payback, ActualDebitedEvent.class);
            producedEvents.add(actualDebitedEvent);
            producedEvents.addAll(toAccountBean.handleAccountDebitedEvent(actualDebitedEvent));

            PaybackCreditEvent paybackCredit = UserAccount.filterEvent(payback, PaybackCreditEvent.class);
            otherPaybackCreditEvents.add(paybackCredit);
            otherPaybackCreditEvents.add(UserAccount.filterEvent(payback, ActualCreditedEvent.class));
        }
    }
}
