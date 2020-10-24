package com.ocbc.bank.events;

import com.ocbc.bank.dto.EventType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@ToString
public class BalanceUpdatedEvent implements UserAccountEvent{
    private final String accountId;
    private final BigDecimal beforeAmount;
    private final BigDecimal amount;
    private LocalDateTime eventTime = LocalDateTime.now();


    @Override
    public EventType eventType() {
        return EventType.BALANCE_UPDATE;
    }

}
