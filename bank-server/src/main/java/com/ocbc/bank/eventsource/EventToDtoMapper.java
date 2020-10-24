package com.ocbc.bank.eventsource;

import com.ocbc.bank.dto.EventType;
import com.ocbc.bank.dto.TransactionLog;
import com.ocbc.bank.events.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class EventToDtoMapper {
    private Map<EventType, Function<TransactionLog, UserAccountEvent>> logToEventMappers = new HashMap();
    private Map<EventType, Function<? extends UserAccountEvent, TransactionLog>> eventToLogMappers = new HashMap();

    public EventToDtoMapper() {
        logToEventMappers.put(EventType.CNF_CREDIT, (log) -> new ActualCreditedEvent(log.getAccountId(), log.getToAccountId(), log.getAmount(),log.getCreateTime()));
        logToEventMappers.put(EventType.TOPUP, (log) -> new TopUpCompletedEvent(log.getAccountId(), log.getAmount(),log.getCreateTime()));
        logToEventMappers.put(EventType.CNF_DEBIT, (log) -> new ActualDebitedEvent(log.getAccountId(), log.getToAccountId(), log.getAmount(),log.getCreateTime()));
        logToEventMappers.put(EventType.CNF_PAYBACK_CREDIT, (log) -> new PaybackCreditEvent(log.getAccountId(), log.getToAccountId(), log.getAmount(),log.getCreateTime()));
        logToEventMappers.put(EventType.CNF_PAYBACK_DEBIT, (log) -> new PaybackDebitEvent(log.getAccountId(), log.getToAccountId(), log.getAmount(),log.getCreateTime()));
        logToEventMappers.put(EventType.PENDING_CREDIT, (log) -> new PendingCreditedEvent(log.getAccountId(), log.getToAccountId(), log.getAmount(),log.getCreateTime()));
        logToEventMappers.put(EventType.PENDING_DEBIT, (log) -> new PendingDebitedEvent(log.getAccountId(), log.getToAccountId(), log.getAmount(),log.getCreateTime()));
    }

    public TransactionLog getLog(UserAccountEvent userAccountEvent) {
        return userAccountEvent.getTransactionLog();
    }

    public UserAccountEvent getEvent(TransactionLog transactionLog) {
        return this.logToEventMappers.get(EventType.valueOf(transactionLog.getTransactionType())).apply(transactionLog);
    }
}
