package com.ocbc.bank.command.dispatcher;

import com.google.common.collect.ImmutableSet;
import com.ocbc.bank.command.model.PayoutDebtsInstruction;
import com.ocbc.bank.domain.DomainOperationResult;
import com.ocbc.bank.domain.UserAccount;
import com.ocbc.bank.domain.UserAccountRepository;
import com.ocbc.bank.domain.util.BalanceAmountMath;
import com.ocbc.bank.events.*;
import com.ocbc.bank.exceptions.BankOperationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CreditSideTransferEventsHandlerTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    private CreditSideTransferEventsHandler creditSideTransferEventsHandler;

    @Mock
    private UserAccount userAccount1;
    @Mock
    private UserAccount userAccount2;
    @Mock
    private UserAccount userAccount3;


    private String accountId1 = "TEST1";
    private String accountId2 = "TEST2";
    private String accountId3 = "TEST3";

    @Before
    public void setup() {
        AsyncEventDispatcher asyncEventDispatcher = mock(AsyncEventDispatcher.class);
        when(asyncEventDispatcher.dispatch(any(Runnable.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return true;
        });
        this.creditSideTransferEventsHandler = new CreditSideTransferEventsHandler(userAccountRepository,
                asyncEventDispatcher);

        when(userAccount1.getReentrantLock()).thenReturn(new ReentrantLock());
        when(userAccount2.getReentrantLock()).thenReturn(new ReentrantLock());

        when(userAccount1.getAccountId()).thenReturn(accountId1);
        when(userAccount2.getAccountId()).thenReturn(accountId2);
        when(userAccount3.getAccountId()).thenReturn(accountId3);

        when(userAccountRepository.getUserAccount(accountId1)).thenReturn(userAccount1);
        when(userAccountRepository.getUserAccount(accountId2)).thenReturn(userAccount2);
        when(userAccountRepository.getUserAccount(accountId3)).thenReturn(userAccount3);

        when(userAccountRepository.getIfPresent(accountId2)).thenReturn(userAccount2);
        when(userAccountRepository.getIfPresent(accountId3)).thenReturn(userAccount3);
    }

    @Test
    public void handleCreditEvents() {
        ActualCreditedEvent actualCreditedEvent = mock(ActualCreditedEvent.class);
        BalanceUpdatedEvent account1BalanceUpdated = new BalanceUpdatedEvent(accountId1, BigDecimal.valueOf(100), BalanceAmountMath.get("0"));
        when(userAccount1.handleAccountCreditedEvent(actualCreditedEvent)).thenReturn(new DomainOperationResult(ImmutableSet.of(account1BalanceUpdated)));

        Set<UserAccountEvent> events = creditSideTransferEventsHandler.handleCreditEvents(userAccount1.getAccountId(), ImmutableSet.of(actualCreditedEvent));

        assertEquals(2, events.size());

        assertEquals(Arrays.asList(actualCreditedEvent, account1BalanceUpdated), new ArrayList(events));
    }


    @Test
    public void handleCreditEventsWithTriggeredPayback() {
        ActualCreditedEvent actualCreditedEvent = mock(ActualCreditedEvent.class);
        BalanceUpdatedEvent account1BalanceUpdated = new BalanceUpdatedEvent(accountId1, BigDecimal.valueOf(100), BalanceAmountMath.get("0"));
        PayoutDebtsInstruction payoutDebtsInstruction = new PayoutDebtsInstruction(accountId1, accountId2, BalanceAmountMath.get("30"));
        PaybackDebitEvent paybackDebitEvent = new PaybackDebitEvent(accountId1, accountId2, payoutDebtsInstruction.getAmount());
        PaybackCreditEvent paybackCreditEvent = new PaybackCreditEvent(accountId2, accountId1, payoutDebtsInstruction.getAmount());
        ActualDebitedEvent actualDebitedEvent1 = new ActualDebitedEvent(accountId1, accountId2, payoutDebtsInstruction.getAmount());
        ActualCreditedEvent actualCreditedEvent1 = new ActualCreditedEvent(accountId2, accountId1, payoutDebtsInstruction.getAmount());

        when(userAccount1.handleAccountCreditedEvent(actualCreditedEvent)).thenReturn(
                new DomainOperationResult(ImmutableSet.of(account1BalanceUpdated), ImmutableSet.of(payoutDebtsInstruction)));

        when(userAccount2.handleAccountCreditedEvent(actualCreditedEvent1)).thenReturn(
                new DomainOperationResult(ImmutableSet.of(account1BalanceUpdated)));

        when(userAccount1.payback(payoutDebtsInstruction)).thenReturn(ImmutableSet.of(paybackDebitEvent, paybackCreditEvent, actualDebitedEvent1, actualCreditedEvent1));

        Set<UserAccountEvent> events = creditSideTransferEventsHandler.handleCreditEvents(userAccount1.getAccountId(), ImmutableSet.of(actualCreditedEvent));

        assertEquals(4, events.size());

        assertEquals(Arrays.asList(actualCreditedEvent, account1BalanceUpdated, paybackDebitEvent, actualDebitedEvent1), new ArrayList(events));
    }


    @Test
    public void handleCreditEventsWithPendingPayments() {
        ActualCreditedEvent actualCreditedEvent = mock(ActualCreditedEvent.class);
        PendingCreditedEvent pendingCreditedEvent = mock(PendingCreditedEvent.class);
        OwingFromUpdatedEvent owingFromUpdatedEvent = mock(OwingFromUpdatedEvent.class);
        BalanceUpdatedEvent account1BalanceUpdated = new BalanceUpdatedEvent(accountId1, BigDecimal.valueOf(100), BalanceAmountMath.get("0"));
        when(userAccount1.handleAccountCreditedEvent(actualCreditedEvent)).thenReturn(new DomainOperationResult(ImmutableSet.of(account1BalanceUpdated)));
        when(userAccount1.handlePendingCreditedEvent(pendingCreditedEvent)).thenReturn(ImmutableSet.of(owingFromUpdatedEvent));
        Set<UserAccountEvent> events = creditSideTransferEventsHandler.handleCreditEvents(userAccount1.getAccountId(), ImmutableSet.of(actualCreditedEvent, pendingCreditedEvent));

        assertEquals(4, events.size());

        assertEquals(Arrays.asList(actualCreditedEvent, account1BalanceUpdated, pendingCreditedEvent, owingFromUpdatedEvent), new ArrayList(events));
    }

    @Test
    public void handleCreditEventsWithException() {
        ActualCreditedEvent actualCreditedEvent = mock(ActualCreditedEvent.class);
        BalanceUpdatedEvent account1BalanceUpdated = new BalanceUpdatedEvent(accountId1, BigDecimal.valueOf(100), BalanceAmountMath.get("0"));
        PayoutDebtsInstruction payoutDebtsInstruction = new PayoutDebtsInstruction(accountId1, accountId2, BalanceAmountMath.get("30"));
        PaybackDebitEvent paybackDebitEvent = new PaybackDebitEvent(accountId1, accountId2, payoutDebtsInstruction.getAmount());
        PaybackCreditEvent paybackCreditEvent = new PaybackCreditEvent(accountId2, accountId1, payoutDebtsInstruction.getAmount());

        when(userAccount1.handleAccountCreditedEvent(actualCreditedEvent)).thenThrow(new IllegalStateException());

        Collection<UserAccountEvent> events;
        try {
            events = creditSideTransferEventsHandler.handleCreditEvents(userAccount1.getAccountId(), ImmutableSet.of(actualCreditedEvent));
        } catch (BankOperationException exception) {
            events = exception.getEvents();
        }

        assertEquals(1, events.size());

        assertEquals(Arrays.asList(actualCreditedEvent), new ArrayList(events));

        verify(userAccount1, times(1)).forceRecovery();
    }


    @Test
    public void handleCreditEventsWithExceptionOnPayouts() {
        ActualCreditedEvent actualCreditedEvent = mock(ActualCreditedEvent.class);
        BalanceUpdatedEvent account1BalanceUpdated = new BalanceUpdatedEvent(accountId1, BigDecimal.valueOf(100), BalanceAmountMath.get("0"));
        PayoutDebtsInstruction payoutDebtsInstruction = new PayoutDebtsInstruction(accountId1, accountId2, BalanceAmountMath.get("30"));
        PaybackDebitEvent paybackDebitEvent = new PaybackDebitEvent(accountId1, accountId2, payoutDebtsInstruction.getAmount());
        PaybackCreditEvent paybackCreditEvent = new PaybackCreditEvent(accountId2, accountId1, payoutDebtsInstruction.getAmount());
        ActualDebitedEvent actualDebitedEvent1 = new ActualDebitedEvent(accountId1, accountId2, payoutDebtsInstruction.getAmount());
        ActualCreditedEvent actualCreditedEvent1 = new ActualCreditedEvent(accountId2, accountId1, payoutDebtsInstruction.getAmount());

        PayoutDebtsInstruction payoutDebtsInstruction2 = new PayoutDebtsInstruction(accountId1, accountId3, BalanceAmountMath.get("30"));

        when(userAccount1.handleAccountCreditedEvent(actualCreditedEvent)).thenReturn(new DomainOperationResult(ImmutableSet.of(account1BalanceUpdated),
                ImmutableSet.of(payoutDebtsInstruction, payoutDebtsInstruction2)));

        when(userAccount1.payback(payoutDebtsInstruction)).thenReturn(ImmutableSet.of(paybackDebitEvent, paybackCreditEvent, actualDebitedEvent1, actualCreditedEvent1));
        when(userAccount1.payback(payoutDebtsInstruction2)).thenThrow(new IllegalStateException());

        Collection<UserAccountEvent> events;
        try {
            events = creditSideTransferEventsHandler.handleCreditEvents(userAccount1.getAccountId(), ImmutableSet.of(actualCreditedEvent));
        } catch (BankOperationException exception) {
            events = exception.getEvents();
        }

        assertEquals(4, events.size());

        assertEquals(Arrays.asList(actualCreditedEvent, account1BalanceUpdated,paybackDebitEvent, actualDebitedEvent1), new ArrayList(events));

        verify(userAccount1, times(1)).forceRecovery();
        verify(userAccount2, times(1)).forceRecovery();
        verify(userAccount3, times(1)).forceRecovery();
    }

}