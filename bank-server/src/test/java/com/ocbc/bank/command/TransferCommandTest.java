package com.ocbc.bank.command;

import com.google.common.collect.ImmutableSet;
import com.ocbc.bank.command.dispatcher.AsyncEventDispatcher;
import com.ocbc.bank.command.dispatcher.CreditSideTransferEventsHandler;
import com.ocbc.bank.command.model.TransferInstruction;
import com.ocbc.bank.domain.UserAccount;
import com.ocbc.bank.domain.UserAccountRepository;
import com.ocbc.bank.domain.util.BalanceAmountMath;
import com.ocbc.bank.dto.BankCommandResult;
import com.ocbc.bank.events.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TransferCommandTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private CreditSideTransferEventsHandler creditSideTransferEventsHandler;

    private TransferCommand transferCommand;

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
        this.transferCommand = new TransferCommand(userAccountRepository, creditSideTransferEventsHandler, asyncEventDispatcher);

        when(userAccount1.getReentrantLock()).thenReturn(new ReentrantLock());
        when(userAccountRepository.getUserAccount(accountId1)).thenReturn(userAccount1);
        when(userAccountRepository.getUserAccount(accountId2)).thenReturn(userAccount2);
    }


    @Test
    public void executeWithBasicAtoBTransfer() {
        TransferInstruction transferInstruction = new TransferInstruction(accountId1, accountId2, BalanceAmountMath.get("100"));

        ActualDebitedEvent actualDebitedEvent = new ActualDebitedEvent(accountId1, accountId2, BalanceAmountMath.get("100"));
        ActualCreditedEvent actualCreditedEvent = new ActualCreditedEvent(accountId2, accountId1, BalanceAmountMath.get("100"));
        BalanceUpdatedEvent account1BalanceUpdated = new BalanceUpdatedEvent(accountId1, BalanceAmountMath.get("200"), BalanceAmountMath.get("100"));

        when(userAccount1.transfer(eq(transferInstruction))).thenReturn(ImmutableSet.of(actualDebitedEvent, actualCreditedEvent));
        when(userAccount1.handleAccountDebitedEvent(actualDebitedEvent)).thenReturn(ImmutableSet.of(account1BalanceUpdated));

        BankCommandResult bankCommandResult = transferCommand.execute(transferInstruction);
        assertTrue(bankCommandResult.isSuccess());
        List<UserAccountEvent> userAccountEvents = bankCommandResult.getUserAccountEvents();
        assertEquals(2, userAccountEvents.size());

        assertEquals(Arrays.asList(actualDebitedEvent, account1BalanceUpdated), new ArrayList(userAccountEvents));
    }

    @Test
    public void executeWithBasicAtoBTransferWithException() {
        TransferInstruction transferInstruction = new TransferInstruction(accountId1, accountId2, BalanceAmountMath.get("100"));

        ActualDebitedEvent actualDebitedEvent = new ActualDebitedEvent(accountId1, accountId2, BalanceAmountMath.get("100"));
        BalanceUpdatedEvent account1BalanceUpdated = new BalanceUpdatedEvent(accountId1, BalanceAmountMath.get("200"), BalanceAmountMath.get("100"));

        when(userAccount1.transfer(eq(transferInstruction))).thenThrow(new IllegalStateException());

        BankCommandResult bankCommandResult = transferCommand.execute(transferInstruction);
        assertFalse(bankCommandResult.isSuccess());
        List<UserAccountEvent> userAccountEvents = bankCommandResult.getUserAccountEvents();
        assertEquals(0, userAccountEvents.size());
        assertTrue(bankCommandResult.getErrorMessage().isPresent());

        verify(userAccount1, times(1)).forceRecovery();
    }

    @Test
    public void executeWithBasicAtoBTransferWithExceptionPostTransfer() {
        TransferInstruction transferInstruction = new TransferInstruction(accountId1, accountId2, BalanceAmountMath.get("100"));

        ActualDebitedEvent actualDebitedEvent = new ActualDebitedEvent(accountId1, accountId2, BalanceAmountMath.get("100"));
        ActualCreditedEvent actualCreditedEvent = new ActualCreditedEvent(accountId2, accountId1, BalanceAmountMath.get("100"));

        when(userAccount1.transfer(eq(transferInstruction))).thenReturn(ImmutableSet.of(actualDebitedEvent, actualCreditedEvent));
        when(userAccount1.handleAccountDebitedEvent(actualDebitedEvent)).thenThrow(new IllegalStateException());

        BankCommandResult bankCommandResult = transferCommand.execute(transferInstruction);
        assertFalse(bankCommandResult.isSuccess());
        List<UserAccountEvent> userAccountEvents = bankCommandResult.getUserAccountEvents();
        assertEquals(1, userAccountEvents.size());
        assertTrue(bankCommandResult.getErrorMessage().isPresent());

        verify(userAccount1, times(1)).forceRecovery();
        assertEquals(Arrays.asList(actualDebitedEvent), new ArrayList(userAccountEvents));
    }


    @Test
    public void executeWithBasicAtoBTransferWithPendingPayments() {
        TransferInstruction transferInstruction = new TransferInstruction(accountId1, accountId2, BigDecimal.valueOf(100));

        ActualDebitedEvent actualDebitedEvent = mock(ActualDebitedEvent.class);
        ActualCreditedEvent actualCreditedEvent = mock(ActualCreditedEvent.class);
        PendingDebitedEvent pendingDebitedEvent = mock(PendingDebitedEvent.class);
        PendingCreditedEvent pendingCreditedEvent = mock(PendingCreditedEvent.class);

        BalanceUpdatedEvent account1BalanceUpdated = new BalanceUpdatedEvent(accountId1, BigDecimal.valueOf(30), BalanceAmountMath.get("0"));
        OwingToUpdatedEvent owingToUpdatedEvent = new OwingToUpdatedEvent(accountId1, accountId2, BalanceAmountMath.get("70"));

        when(userAccount1.transfer(eq(transferInstruction))).thenReturn(ImmutableSet.of(pendingDebitedEvent, pendingCreditedEvent, actualDebitedEvent, actualCreditedEvent));
        when(userAccount1.handlePendingDebitedEvent(pendingDebitedEvent)).thenReturn(ImmutableSet.of(owingToUpdatedEvent));
        when(userAccount1.handleAccountDebitedEvent(actualDebitedEvent)).thenReturn(ImmutableSet.of(account1BalanceUpdated));


        BankCommandResult bankCommandResult = transferCommand.execute(transferInstruction);
        assertTrue(bankCommandResult.isSuccess());
        List<UserAccountEvent> userAccountEvents = bankCommandResult.getUserAccountEvents();
        assertEquals(4, userAccountEvents.size());

        assertEquals(Arrays.asList(actualDebitedEvent, account1BalanceUpdated,
                pendingDebitedEvent, owingToUpdatedEvent), new ArrayList(userAccountEvents));
    }

}