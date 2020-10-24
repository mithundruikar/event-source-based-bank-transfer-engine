package com.ocbc.bank.events;

import com.ocbc.bank.dto.EventType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@EqualsAndHashCode
@ToString
public class PendingDebitedEvent implements UserAccountEvent {
    private final String accountId;
    private final String otherAccountId;
    private final BigDecimal amount;
    private LocalDateTime eventTime = LocalDateTime.now();
    private final String direction = "D";

    public PendingDebitedEvent(String accountId, String otherAccountId, BigDecimal amount) {
        this.accountId = accountId;
        this.otherAccountId = otherAccountId;
        this.amount = amount;
    }

    public PendingDebitedEvent(String accountId, String otherAccountId, BigDecimal amount, LocalDateTime eventTime) {
        this.accountId = accountId;
        this.otherAccountId = otherAccountId;
        this.amount = amount;
        this.eventTime = eventTime;
    }

    @Override
    public EventType eventType() {
        return EventType.PENDING_DEBIT;
    }
}
