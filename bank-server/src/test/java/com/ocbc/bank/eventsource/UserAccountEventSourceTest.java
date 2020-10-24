package com.ocbc.bank.eventsource;

import com.ocbc.bank.domain.UserAccount;
import com.ocbc.bank.domain.util.BalanceAmountMath;
import com.ocbc.bank.dto.EventType;
import com.ocbc.bank.dto.TransactionLog;
import com.ocbc.bank.dto.TransactionLogRepository;
import com.ocbc.bank.events.*;
import com.ocbc.bank.exceptions.BankOperationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserAccountEventSourceTest {

    @Mock
    private TransactionLogRepository transactionLogRepository;

    private UserAccountEventSource userAccountEventSource;

    @Before
    public void setup() {
        this.userAccountEventSource = new UserAccountEventSource(transactionLogRepository, new EventToDtoMapper());
    }

    @Test
    public void getEventsByAccount() {
        LocalDateTime eventTime = LocalDateTime.now();
        TransactionLog log1 = new TransactionLog("TESTACC1", "TOACC", EventType.CNF_DEBIT.name(), BigDecimal.valueOf(100), "D", eventTime);
        TransactionLog log2 = new TransactionLog("TESTACC1", "TOACC", EventType.CNF_CREDIT.name(), BigDecimal.valueOf(100), "C", eventTime);
        TransactionLog log3 = new TransactionLog("TESTACC1", "TOACC", EventType.PENDING_DEBIT.name(), BigDecimal.valueOf(100), "D", eventTime);
        TransactionLog log4 = new TransactionLog("TESTACC1", "TOACC", EventType.PENDING_CREDIT.name(), BigDecimal.valueOf(100), "C", eventTime);
        TransactionLog log5 = new TransactionLog("TESTACC1", "TOACC", EventType.CNF_PAYBACK_DEBIT.name(), BigDecimal.valueOf(100), "D", eventTime);
        TransactionLog log6 = new TransactionLog("TESTACC1", "TOACC", EventType.CNF_PAYBACK_CREDIT.name(), BigDecimal.valueOf(100), "C", eventTime);
        TransactionLog log7 = new TransactionLog("TESTACC1", "TESTACC1", EventType.TOPUP.name(), BigDecimal.valueOf(100), "C", eventTime);

        ActualCreditedEvent event1 = new ActualCreditedEvent("TESTACC1", "TOACC", BigDecimal.valueOf(100), eventTime);
        ActualDebitedEvent event2 = new ActualDebitedEvent("TESTACC1", "TOACC", BigDecimal.valueOf(100), eventTime);
        PendingDebitedEvent event3 = new PendingDebitedEvent("TESTACC1", "TOACC", BigDecimal.valueOf(100), eventTime);
        PendingCreditedEvent event4 = new PendingCreditedEvent("TESTACC1", "TOACC", BigDecimal.valueOf(100), eventTime);
        PaybackDebitEvent event5 = new PaybackDebitEvent("TESTACC1", "TOACC", BigDecimal.valueOf(100), eventTime);
        PaybackCreditEvent event6 = new PaybackCreditEvent("TESTACC1", "TOACC", BigDecimal.valueOf(100), eventTime);
        TopUpCompletedEvent event7 = new TopUpCompletedEvent("TESTACC1", BigDecimal.valueOf(100), eventTime);

        when(this.transactionLogRepository.getOrderedLogs("TESTACC1")).thenReturn(Arrays.asList(log1, log2, log3, log4, log5, log6, log7));
        List<UserAccountEvent> testacc1 = this.userAccountEventSource.getEvents("TESTACC1");
        assertEquals(7, testacc1.size());
        assertTrue(testacc1.contains(event1));
        assertTrue(testacc1.contains(event2));
        assertTrue(testacc1.contains(event3));
        assertTrue(testacc1.contains(event4));
        assertTrue(testacc1.contains(event5));
        assertTrue(testacc1.contains(event6));
        assertTrue(testacc1.contains(event7));
    }

    @Test
    public void eventSource() {
        // TESTACC1 topped up with balance 100
        // TESTACC1 pays 150 to TOACC1. 100 confirmed and 50 pending.
        // TESTACC1 receives confirmed 300 from TOACC2
        // TESTACC1 waiting to receive 100 from TOACC2
        // TESTACC1 pays back 50 from TOACC1

        LocalDateTime eventTime = LocalDateTime.now();
        TransactionLog log0 = new TransactionLog("TESTACC1", "TESTACC1", EventType.TOPUP.name(), BigDecimal.valueOf(100), "C", eventTime);
        TransactionLog log1 = new TransactionLog("TESTACC1", "TOACC1", EventType.CNF_DEBIT.name(), BigDecimal.valueOf(100), "D", eventTime);
        TransactionLog log2 = new TransactionLog("TESTACC1", "TOACC1", EventType.PENDING_DEBIT.name(), BigDecimal.valueOf(50), "C", eventTime);
        TransactionLog log3 = new TransactionLog("TESTACC1", "TOACC2", EventType.CNF_CREDIT.name(), BigDecimal.valueOf(300), "D", eventTime);
        TransactionLog log4 = new TransactionLog("TESTACC1", "TOACC2", EventType.PENDING_CREDIT.name(), BigDecimal.valueOf(100), "C", eventTime);
        TransactionLog log5 = new TransactionLog("TESTACC1", "TOACC1", EventType.CNF_PAYBACK_DEBIT.name(), BigDecimal.valueOf(50), "C", eventTime);
        TransactionLog log6 = new TransactionLog("TESTACC1", "TOACC1", EventType.CNF_DEBIT.name(), BigDecimal.valueOf(50), "C", eventTime);

        when(this.transactionLogRepository.getOrderedLogs("TESTACC1")).thenReturn(Arrays.asList(log0, log1, log2, log3, log4, log5, log6));
        UserAccount userAccount = new UserAccount("TESTACC1", this.userAccountEventSource);

        assertEquals(BalanceAmountMath.get("250"), userAccount.getBalance());
        assertTrue(userAccount.isRecovered());
    }


    @Test(expected = BankOperationException.class)
    public void eventSourceWithInvalidOrder() {
        LocalDateTime eventTime = LocalDateTime.now();
        TransactionLog log5 = new TransactionLog("TESTACC1", "TOACC1", EventType.CNF_PAYBACK_DEBIT.name(), BigDecimal.valueOf(50), "C", eventTime);

        when(this.transactionLogRepository.getOrderedLogs("TESTACC1")).thenReturn(Arrays.asList(log5));
        new UserAccount("TESTACC1", this.userAccountEventSource);
        fail();
    }
}