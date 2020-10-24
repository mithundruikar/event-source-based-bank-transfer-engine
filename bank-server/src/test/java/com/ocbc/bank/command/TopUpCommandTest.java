package com.ocbc.bank.command;

import com.google.common.collect.ImmutableSet;
import com.ocbc.bank.command.dispatcher.CreditSideTransferEventsHandler;
import com.ocbc.bank.command.model.PayoutDebtsInstruction;
import com.ocbc.bank.command.model.TopUpAccount;
import com.ocbc.bank.domain.UserAccount;
import com.ocbc.bank.domain.UserAccountRepository;
import com.ocbc.bank.domain.util.BalanceAmountMath;
import com.ocbc.bank.dto.BankCommandResult;
import com.ocbc.bank.events.*;

import com.ocbc.bank.exceptions.UserAccountInvalidStateException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TopUpCommandTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private CreditSideTransferEventsHandler creditSideTransferEventsHandler;
    @Mock
    private UserAccount userAccount1;
    @Mock
    private UserAccount userAccount2;
    @Mock
    private UserAccount userAccount3;

    private TopUpCommand topUpCommand;
    private String accountId1 = "TEST1";
    private String accountId2 = "TEST2";
    private String accountId3 = "TEST3";

    @Before
    public void setup() {
        this.topUpCommand = new TopUpCommand(userAccountRepository, creditSideTransferEventsHandler);

        when(userAccount1.getReentrantLock()).thenReturn(new ReentrantLock());
        when(userAccountRepository.getUserAccount(accountId1)).thenReturn(userAccount1);
    }

    @Test
    public void topup() {
        TopUpAccount topUpAccount = new TopUpAccount(accountId1, BalanceAmountMath.get("100"));
        TopUpCompletedEvent topUpCompletedEvent = new TopUpCompletedEvent(accountId1, BalanceAmountMath.get("100"));
        when(this.userAccount1.topUp(topUpAccount)).thenReturn(topUpCompletedEvent);

        BalanceUpdatedEvent balanceUpdatedEvent = new BalanceUpdatedEvent(accountId1, BalanceAmountMath.get("0"), BalanceAmountMath.get("100"));

        when(this.creditSideTransferEventsHandler.handleCreditEvents(eq(this.userAccount1.getAccountId()), eq(ImmutableSet.of(topUpCompletedEvent))))
                .thenReturn(ImmutableSet.of(balanceUpdatedEvent));

        BankCommandResult bankCommandResult = topUpCommand.execute(topUpAccount);
        assertTrue(bankCommandResult.isSuccess());
        List<UserAccountEvent> result = bankCommandResult.getUserAccountEvents();

        assertEquals(2, result.size());

        assertEquals(Arrays.asList(topUpCompletedEvent, balanceUpdatedEvent), new ArrayList(result));
    }

    @Test
    public void topupWithException() {
        TopUpAccount topUpAccount = new TopUpAccount(accountId1, BalanceAmountMath.get("100"));
        TopUpCompletedEvent topUpCompletedEvent = new TopUpCompletedEvent(accountId1, BalanceAmountMath.get("100"));
        when(this.userAccount1.topUp(topUpAccount)).thenThrow(new IllegalStateException());

        BalanceUpdatedEvent balanceUpdatedEvent = new BalanceUpdatedEvent(accountId1, BalanceAmountMath.get("0"), BalanceAmountMath.get("100"));

        BankCommandResult bankCommandResult = topUpCommand.execute(topUpAccount);
        assertFalse(bankCommandResult.isSuccess());
        assertTrue(bankCommandResult.getErrorMessage().isPresent());
        List<UserAccountEvent> result = bankCommandResult.getUserAccountEvents();

        assertEquals(0, result.size());

        verify(userAccount1, times(1)).forceRecovery();
    }

    @Test
    public void topupWithExceptionPostTopup() {
        TopUpAccount topUpAccount = new TopUpAccount(accountId1, BalanceAmountMath.get("100"));
        TopUpCompletedEvent topUpCompletedEvent = new TopUpCompletedEvent(accountId1, BalanceAmountMath.get("100"));
        when(this.userAccount1.topUp(topUpAccount)).thenReturn(topUpCompletedEvent);

        when(this.creditSideTransferEventsHandler.handleCreditEvents(eq(this.userAccount1.getAccountId()), eq(ImmutableSet.of(topUpCompletedEvent))))
                .thenThrow(new IllegalStateException());

        BankCommandResult bankCommandResult = topUpCommand.execute(topUpAccount);
        assertFalse(bankCommandResult.isSuccess());
        assertTrue(bankCommandResult.getErrorMessage().isPresent());
        List<UserAccountEvent> result = bankCommandResult.getUserAccountEvents();

        assertEquals(1, result.size());
        assertEquals(Arrays.asList(topUpCompletedEvent), new ArrayList(result));

        verify(userAccount1, times(1)).forceRecovery();
    }


    @Test
    public void topupWithPayouts() throws UserAccountInvalidStateException {
        TopUpAccount topUpAccount = new TopUpAccount(accountId1, BalanceAmountMath.get("100"));
        TopUpCompletedEvent topUpCompletedEvent = new TopUpCompletedEvent(accountId1, BalanceAmountMath.get("100"));
        when(this.userAccount1.topUp(topUpAccount)).thenReturn(topUpCompletedEvent);

        BalanceUpdatedEvent balanceUpdatedEvent = new BalanceUpdatedEvent(accountId1, BalanceAmountMath.get("0"), BalanceAmountMath.get("100"));
        PayoutDebtsInstruction payoutDebtsInstruction = new PayoutDebtsInstruction(accountId1, accountId2, BalanceAmountMath.get("30"));
        PaybackDebitEvent paybackDebitEvent = new PaybackDebitEvent(accountId2, accountId3, payoutDebtsInstruction.getAmount());
        PaybackCreditEvent paybackCreditEvent = new PaybackCreditEvent(accountId3, accountId2, payoutDebtsInstruction.getAmount());

        BalanceUpdatedEvent balanceUpdatedPostPayoutEvent = new BalanceUpdatedEvent(accountId1, BalanceAmountMath.get("100"), BalanceAmountMath.get("70"));
        OwingToUpdatedEvent owingToUpdatedEvent = new OwingToUpdatedEvent(accountId1, accountId2, BalanceAmountMath.get("0"));

        when(this.creditSideTransferEventsHandler.handleCreditEvents(eq(this.userAccount1.getAccountId()), eq(ImmutableSet.of(topUpCompletedEvent))))
                .thenReturn(ImmutableSet.of(balanceUpdatedEvent, paybackDebitEvent,
                        balanceUpdatedPostPayoutEvent, owingToUpdatedEvent));

        BankCommandResult bankCommandResult = topUpCommand.execute(topUpAccount);
        assertTrue(bankCommandResult.isSuccess());
        List<UserAccountEvent> result = bankCommandResult.getUserAccountEvents();

        assertEquals(5, result.size());

        assertEquals(Arrays.asList(topUpCompletedEvent, balanceUpdatedEvent, paybackDebitEvent,
                balanceUpdatedPostPayoutEvent, owingToUpdatedEvent), new ArrayList(result));
    }
}