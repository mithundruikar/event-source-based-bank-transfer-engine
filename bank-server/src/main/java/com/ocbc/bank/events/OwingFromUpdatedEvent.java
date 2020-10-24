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
public class OwingFromUpdatedEvent implements UserAccountEvent {
    private final String accountId;
    private final String otherAccountId;
    private final BigDecimal amount;
    private LocalDateTime eventTime = LocalDateTime.now();

    @Override
    public EventType eventType() {
        return EventType.OWING_FROM_UPDATE;
    }

}
