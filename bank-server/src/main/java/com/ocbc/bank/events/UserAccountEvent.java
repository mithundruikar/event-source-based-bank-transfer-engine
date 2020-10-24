package com.ocbc.bank.events;

import com.ocbc.bank.dto.BankOperationResult;
import com.ocbc.bank.dto.EventType;
import com.ocbc.bank.dto.TransactionLog;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Base type for all domain events generated on state change of the {@link com.ocbc.bank.domain.UserAccount} domain aggregate
 * There are 2 types of events. <br>
 * i) Events which are logged in the event logs atomically e.g. {@link ActualDebitedEvent}<br>
 * ii) Events which are status updates. and not persisted. e.g. {@link BalanceUpdatedEvent}<br>
 *
 */
public interface UserAccountEvent {
    String getAccountId();

    EventType eventType();

    default BankOperationResult map() {
        return new BankOperationResult(eventType().name(), getOtherAccountId(), getAmount());
    }

    default String getOtherAccountId() {
        return null;
    }

    BigDecimal getAmount();

    default String getDirection() {
        return "";
    }

    LocalDateTime getEventTime();

    default TransactionLog getTransactionLog() {
        return new TransactionLog(getAccountId(), getOtherAccountId(), eventType().name(), getAmount(), getDirection(), getEventTime());
    }

}
