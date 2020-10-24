package com.ocbc.bank.events;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@EqualsAndHashCode
@ToString
public class TopUpCompletedEvent extends ActualCreditedEvent {

    public TopUpCompletedEvent(String accountId, BigDecimal amount) {
        super(accountId, accountId, amount);
    }

    public TopUpCompletedEvent(String accountId, BigDecimal amount, LocalDateTime eventTime) {
        super(accountId, accountId, amount, eventTime);
    }


}
