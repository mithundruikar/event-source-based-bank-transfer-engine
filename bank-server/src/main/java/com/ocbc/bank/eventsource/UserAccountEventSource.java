package com.ocbc.bank.eventsource;

import com.ocbc.bank.domain.UserAccount;
import com.ocbc.bank.dto.TransactionLogRepository;
import com.ocbc.bank.dto.TransactionLog;
import com.ocbc.bank.events.*;
import com.ocbc.bank.exceptions.UserAccountInvalidStateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Event source for {@link UserAccount} domain Aggregate. <br>
 * It uses {@link TransactionLogRepository} to recover events in event time ordered manner
 *
 */
@Slf4j
public class UserAccountEventSource {
    private TransactionLogRepository transactionLogRepository;
    private EventToDtoMapper eventToDtoMapper;

    public UserAccountEventSource(TransactionLogRepository transactionLogRepository, EventToDtoMapper eventToDtoMapper) {
        this.transactionLogRepository = transactionLogRepository;
        this.eventToDtoMapper = eventToDtoMapper;
    }

    public List<UserAccountEvent> getEvents(String accountId) {
        List<TransactionLog> byAccountId = this.transactionLogRepository.getOrderedLogs(accountId);
        return byAccountId.stream().map(this.eventToDtoMapper::getEvent).collect(Collectors.toList());
    }

    @Transactional
    public boolean save(Collection<UserAccountEvent> userAccountEvents) {
        try {
            List<TransactionLog> collect = userAccountEvents.stream().map(UserAccountEvent::getTransactionLog).collect(Collectors.toList());
            this.transactionLogRepository.saveAll(collect);
        } catch(RuntimeException re) {
            log.error("Error while persisting events. recovery needed for owning account domains", re);
            return false;
        }
        return true;
    }


    public void eventSource(UserAccount userAccount) {
        List<UserAccountEvent> recoveredEvents = getEvents(userAccount.getAccountId());
        recoveredEvents.forEach(event -> {
            UserAccount.filterOptionalEvent(event, ActualDebitedEvent.class).ifPresent(userAccount::handleAccountDebitedEvent);
            UserAccount.filterOptionalEvent(event, ActualCreditedEvent.class).ifPresent(userAccount::handleAccountCreditedEvent);
            UserAccount.filterOptionalEvent(event, TopUpCompletedEvent.class).ifPresent(userAccount::handleTopUpCompletedEvent);
            UserAccount.filterOptionalEvent(event, PaybackCreditEvent.class).ifPresent(e -> {
                try {
                    userAccount.handlePaybackCreditEvent(e);
                } catch (UserAccountInvalidStateException exc) {
                    throw new IllegalStateException(exc);
                }
            });
            UserAccount.filterOptionalEvent(event, PaybackDebitEvent.class).ifPresent(e -> {
                try {
                    userAccount.handlePaybackDebitEvent(e);
                } catch (UserAccountInvalidStateException exc) {
                    throw new IllegalStateException(exc);
                }
            });
            UserAccount.filterOptionalEvent(event, PendingCreditedEvent.class).ifPresent(userAccount::handlePendingCreditedEvent);
            UserAccount.filterOptionalEvent(event, PendingDebitedEvent.class).ifPresent(userAccount::handlePendingDebitedEvent);
        });
    }


}

