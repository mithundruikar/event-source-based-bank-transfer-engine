package com.ocbc.bank.domain;


import com.ocbc.bank.command.model.PayoutDebtsInstruction;
import com.ocbc.bank.command.model.TopUpAccount;
import com.ocbc.bank.command.model.TransferInstruction;
import com.ocbc.bank.domain.util.BalanceAmountMath;
import com.ocbc.bank.events.*;

import com.ocbc.bank.eventsource.UserAccountEventSource;
import com.ocbc.bank.exceptions.BankOperationException;
import com.ocbc.bank.exceptions.UserAccountInvalidStateException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserAccountTest {
    private UserAccount userAccount1;
    private UserAccount userAccount2;
    private String accountId1 = "TEST1";
    private String accountId2 = "TEST2";
    private String accountId3 = "TEST3";

    @Mock
    private UserAccountEventSource userAccountEventSource;

    @Before
    public void setUp() throws Exception {
        userAccount1 = new UserAccount(accountId1, userAccountEventSource);
        userAccount2 = new UserAccount(accountId2, userAccountEventSource);
        when(userAccountEventSource.save(any())).thenReturn(true);
    }

    @Test
    public void transfer() {
        TransferInstruction transferInstruction = new TransferInstruction(accountId1, accountId2, BigDecimal.valueOf(100));
        Set<UserAccountEvent> transfers = userAccount1.transfer(transferInstruction);
        assertNotNull(transfers);
        assertEquals(4, transfers.size());

        Iterator<UserAccountEvent> iterator = transfers.iterator();
        PendingDebitedEvent pendingDebitedEvent = (PendingDebitedEvent) iterator.next();
        PendingCreditedEvent pendingCreditedEvent = (PendingCreditedEvent) iterator.next();
        ActualDebitedEvent actualDebitedEvent = (ActualDebitedEvent) iterator.next();
        ActualCreditedEvent actualCreditedEvent = (ActualCreditedEvent) iterator.next();

        assertEquals(transferInstruction.getFromAccountId(), actualDebitedEvent.getAccountId());
        assertEquals(transferInstruction.getToAccountId(), actualDebitedEvent.getOtherAccountId());
        assertEquals(BigDecimal.ZERO.setScale(2), actualDebitedEvent.getAmount());

        assertEquals(transferInstruction.getFromAccountId(), actualCreditedEvent.getOtherAccountId());
        assertEquals(transferInstruction.getToAccountId(), actualCreditedEvent.getAccountId());
        assertEquals(BigDecimal.ZERO.setScale(2), actualCreditedEvent.getAmount());

        assertEquals(transferInstruction.getFromAccountId(), pendingDebitedEvent.getAccountId());
        assertEquals(transferInstruction.getToAccountId(), pendingDebitedEvent.getOtherAccountId());
        assertEquals(BigDecimal.valueOf(100).setScale(2), pendingDebitedEvent.getAmount());


        assertEquals(transferInstruction.getFromAccountId(), pendingCreditedEvent.getOtherAccountId());
        assertEquals(transferInstruction.getToAccountId(), pendingCreditedEvent.getAccountId());
        assertEquals(BigDecimal.valueOf(100).setScale(2), pendingCreditedEvent.getAmount());
    }

    @Test
    public void transferWithoutDebts() {
        userAccount1.setBalance(BigDecimal.valueOf(200));
        TransferInstruction transferInstruction = new TransferInstruction(accountId1, accountId2, BigDecimal.valueOf(100));
        Set<UserAccountEvent> transfers = userAccount1.transfer(transferInstruction);
        assertNotNull(transfers);
        assertEquals(2, transfers.size());

        Iterator<UserAccountEvent> iterator = transfers.iterator();
        ActualDebitedEvent actualDebitedEvent = (ActualDebitedEvent) iterator.next();
        ActualCreditedEvent actualCreditedEvent = (ActualCreditedEvent) iterator.next();

        assertEquals(transferInstruction.getFromAccountId(), actualDebitedEvent.getAccountId());
        assertEquals(transferInstruction.getToAccountId(), actualDebitedEvent.getOtherAccountId());
        assertEquals(BigDecimal.valueOf(100).setScale(2), actualDebitedEvent.getAmount());

        assertEquals(transferInstruction.getFromAccountId(), actualCreditedEvent.getOtherAccountId());
        assertEquals(transferInstruction.getToAccountId(), actualCreditedEvent.getAccountId());
        assertEquals(BigDecimal.valueOf(100).setScale(2), actualCreditedEvent.getAmount());
    }

    @Test
    public void accountDebitedEvent() {
        userAccount1.setBalance(BigDecimal.valueOf(150));

        ActualDebitedEvent transferCompletedEvent = new ActualDebitedEvent(accountId1, accountId2, BigDecimal.valueOf(100));
        Set<UserAccountEvent> userAccountEvents = userAccount1.handleAccountDebitedEvent(transferCompletedEvent);
        assertFalse(userAccountEvents.isEmpty());
        assertEquals(1, userAccountEvents.size());

        BalanceUpdatedEvent balanceUpdatedEvent = UserAccount.filterEvent(userAccountEvents, BalanceUpdatedEvent.class);
        assertEquals(transferCompletedEvent.getAccountId(), balanceUpdatedEvent.getAccountId());
        assertEquals(BalanceAmountMath.get(BigDecimal.valueOf(50)), BalanceAmountMath.get(balanceUpdatedEvent.getAmount()));
    }

    @Test
    public void accountCreditedEvent() {
        ActualCreditedEvent transferCompletedEvent = new ActualCreditedEvent(accountId2, accountId1, BigDecimal.valueOf(100));
        DomainOperationResult domainOperationResult = userAccount2.handleAccountCreditedEvent(transferCompletedEvent);
        Set<UserAccountEvent> userAccountEvents = domainOperationResult.getCompletedEvents();
        assertFalse(userAccountEvents.isEmpty());
        assertEquals(1, userAccountEvents.size());

        BalanceUpdatedEvent balanceUpdatedEvent = UserAccount.filterEvent(userAccountEvents, BalanceUpdatedEvent.class);
        assertEquals(transferCompletedEvent.getAccountId(), balanceUpdatedEvent.getAccountId());
        assertEquals(BigDecimal.valueOf(100).setScale(2), balanceUpdatedEvent.getAmount());
    }

    @Test
    public void accountCreditedEventWithPayouts() {
        userAccount2.setBalance(BigDecimal.ZERO);
        userAccount2.setPendingDebits(accountId3, BalanceAmountMath.get("70"));

        ActualCreditedEvent transferCompletedEvent = new ActualCreditedEvent(accountId2, accountId1, BigDecimal.valueOf(100));
        DomainOperationResult domainOperationResult = userAccount2.handleAccountCreditedEvent(transferCompletedEvent);
        Set<UserAccountEvent> userAccountEvents = domainOperationResult.getCompletedEvents();
        assertFalse(userAccountEvents.isEmpty());
        assertEquals(1, userAccountEvents.size());

        BalanceUpdatedEvent balanceUpdatedEvent = UserAccount.filterEvent(userAccountEvents, BalanceUpdatedEvent.class);
        assertEquals(transferCompletedEvent.getAccountId(), balanceUpdatedEvent.getAccountId());
        assertEquals(BigDecimal.valueOf(100).setScale(2), balanceUpdatedEvent.getAmount());

        Set<PayoutDebtsInstruction> payoutDebtsInstructions = domainOperationResult.getPayoutDebtsInstructions();
        assertFalse(payoutDebtsInstructions.isEmpty());
        assertEquals(1, payoutDebtsInstructions.size());

        PayoutDebtsInstruction payoutDebtsInstruction = payoutDebtsInstructions.iterator().next();
        assertEquals(accountId2, payoutDebtsInstruction.getAccountId());
        assertEquals(accountId3, payoutDebtsInstruction.getToAccountId());
        assertEquals(BigDecimal.valueOf(70).setScale(2), payoutDebtsInstruction.getAmount());
    }

    @Test
    public void topUp() {
        TopUpAccount topUpAccount = new TopUpAccount(accountId1, BigDecimal.valueOf(100));
        TopUpCompletedEvent topUpCompletedEvent = userAccount1.topUp(topUpAccount);
        assertNotNull(topUpCompletedEvent);
    }

    @Test
    public void payback() {
        PayoutDebtsInstruction payoutDebtsInstruction = new PayoutDebtsInstruction(accountId1, accountId2, BigDecimal.valueOf(50));
        Set<UserAccountEvent> payback = userAccount1.payback(payoutDebtsInstruction);
        assertEquals(4, payback.size());

        Iterator<UserAccountEvent> iterator = payback.iterator();
        PaybackDebitEvent paybackDebitedEvent = (PaybackDebitEvent) iterator.next();
        PaybackCreditEvent paybackCreditedEvent = (PaybackCreditEvent) iterator.next();
        ActualDebitedEvent accountDebitedEvent = (ActualDebitedEvent) iterator.next();
        ActualCreditedEvent accountCreditedEvent = (ActualCreditedEvent) iterator.next();

        assertEquals(payoutDebtsInstruction.getAccountId(), paybackDebitedEvent.getAccountId());
        assertEquals(payoutDebtsInstruction.getToAccountId(), paybackDebitedEvent.getOtherAccountId());
        assertEquals(payoutDebtsInstruction.getAmount(), paybackDebitedEvent.getAmount());

        assertEquals(payoutDebtsInstruction.getAccountId(), paybackCreditedEvent.getOtherAccountId());
        assertEquals(payoutDebtsInstruction.getToAccountId(), paybackCreditedEvent.getAccountId());
        assertEquals(payoutDebtsInstruction.getAmount(), paybackDebitedEvent.getAmount());

        assertEquals(payoutDebtsInstruction.getAccountId(), accountDebitedEvent.getAccountId());
        assertEquals(payoutDebtsInstruction.getToAccountId(), accountDebitedEvent.getOtherAccountId());
        assertEquals(payoutDebtsInstruction.getAmount(), accountDebitedEvent.getAmount());

        assertEquals(payoutDebtsInstruction.getAccountId(), accountCreditedEvent.getOtherAccountId());
        assertEquals(payoutDebtsInstruction.getToAccountId(), accountCreditedEvent.getAccountId());
        assertEquals(payoutDebtsInstruction.getAmount(), accountCreditedEvent.getAmount());
    }

    @Test
    public void handleTopupCompletedEvent() {
        //1. account1 has a debt of 100 to account2
        userAccount1.setBalance(BigDecimal.ZERO);
        userAccount1.setPendingDebits(accountId2, BigDecimal.valueOf(100));

        //2. top up account1 with 30
        TopUpCompletedEvent topUpCompletedEvent = new TopUpCompletedEvent(accountId1, BigDecimal.valueOf(30));
        DomainOperationResult payoutDebtsInstructionDomainOperationResult = userAccount1.handleTopUpCompletedEvent(topUpCompletedEvent);
        Set<UserAccountEvent> completedEvents = payoutDebtsInstructionDomainOperationResult.getCompletedEvents();
        Set<PayoutDebtsInstruction> requierdInstructions = payoutDebtsInstructionDomainOperationResult.getPayoutDebtsInstructions();

        //3 expect completed event of balance update
        BalanceUpdatedEvent balanceUpdatedEvent = UserAccount.filterEvent(completedEvents, BalanceUpdatedEvent.class);
        assertEquals(topUpCompletedEvent.getAccountId(), balanceUpdatedEvent.getAccountId());
        assertEquals(BigDecimal.valueOf(30).setScale(2), balanceUpdatedEvent.getAmount());

        //4 expect payout debts instruction to account2 of 30
        List<PayoutDebtsInstruction> payoutDebtsInstructions = UserAccount.filterInstructions(requierdInstructions, PayoutDebtsInstruction.class);
        assertEquals(1, payoutDebtsInstructions.size());
        PayoutDebtsInstruction payoutDebtsInstruction = payoutDebtsInstructions.get(0);
        assertEquals(topUpCompletedEvent.getAccountId(), payoutDebtsInstruction.getAccountId());
        assertEquals(accountId2, payoutDebtsInstruction.getToAccountId());
        assertEquals(BigDecimal.valueOf(30).setScale(2), payoutDebtsInstruction.getAmount());
    }

    @Test
    public void handleTopupCompletedEventWithMultipleDebts() {
        //1. account1 has a debt of 100 to account2
        userAccount1.setBalance(BigDecimal.ZERO);
        userAccount1.setPendingDebits(accountId2, BalanceAmountMath.get("100"));

        //2. account1 has a debt of 30 to account3
        userAccount1.setPendingDebits(accountId3, BalanceAmountMath.get("30"));

        //3. top up account1 with 110
        TopUpCompletedEvent topUpCompletedEvent = new TopUpCompletedEvent(accountId1, BigDecimal.valueOf(110));
        DomainOperationResult payoutDebtsInstructionDomainOperationResult = userAccount1.handleTopUpCompletedEvent(topUpCompletedEvent);
        Set<UserAccountEvent> completedEvents = payoutDebtsInstructionDomainOperationResult.getCompletedEvents();
        Set<PayoutDebtsInstruction> requierdInstructions = payoutDebtsInstructionDomainOperationResult.getPayoutDebtsInstructions();

        //4 expect completed event of balance update
        BalanceUpdatedEvent balanceUpdatedEvent = UserAccount.filterEvent(completedEvents, BalanceUpdatedEvent.class);
        assertEquals(topUpCompletedEvent.getAccountId(), balanceUpdatedEvent.getAccountId());
        assertEquals(BigDecimal.valueOf(110).setScale(2), balanceUpdatedEvent.getAmount());

        //4 expect payout debts instruction to account2 of 100 and account3 of 10
        List<PayoutDebtsInstruction> payoutDebtsInstructions = UserAccount.filterInstructions(requierdInstructions, PayoutDebtsInstruction.class);
        assertEquals(2, payoutDebtsInstructions.size());
        PayoutDebtsInstruction payoutDebtsInstruction1 = payoutDebtsInstructions.get(0);
        assertEquals(topUpCompletedEvent.getAccountId(), payoutDebtsInstruction1.getAccountId());
        assertEquals(accountId2, payoutDebtsInstruction1.getToAccountId());
        assertEquals(BigDecimal.valueOf(100).setScale(2), payoutDebtsInstruction1.getAmount());

        PayoutDebtsInstruction payoutDebtsInstruction2 = payoutDebtsInstructions.get(1);
        assertEquals(topUpCompletedEvent.getAccountId(), payoutDebtsInstruction2.getAccountId());
        assertEquals(accountId3, payoutDebtsInstruction2.getToAccountId());
        assertEquals(BigDecimal.valueOf(10).setScale(2), payoutDebtsInstruction2.getAmount());
    }

    @Test
    public void handlePaybackDebitEvent() throws UserAccountInvalidStateException {
        userAccount1.setBalance(BigDecimal.valueOf(50));
        userAccount1.setPendingDebits(accountId2, BigDecimal.valueOf(40));

        Set<UserAccountEvent> userAccountEvents = userAccount1.handlePaybackDebitEvent(new PaybackDebitEvent(accountId1, accountId2, BigDecimal.valueOf(40)));
        assertEquals(1, userAccountEvents.size());

        OwingToUpdatedEvent owingToUpdatedEvent = UserAccount.filterEvent(userAccountEvents, OwingToUpdatedEvent.class);
        assertEquals(accountId1, owingToUpdatedEvent.getAccountId());
        assertEquals(accountId2, owingToUpdatedEvent.getOtherAccountId());
        assertEquals(BigDecimal.ZERO.setScale(2), owingToUpdatedEvent.getAmount());
    }


    @Test
    public void handlePaybackDebitEventPartialPayout() throws UserAccountInvalidStateException {
        userAccount1.setBalance(BigDecimal.valueOf(50));
        userAccount1.setPendingDebits(accountId2, BigDecimal.valueOf(40));

        Set<UserAccountEvent> userAccountEvents = userAccount1.handlePaybackDebitEvent(new PaybackDebitEvent(accountId1, accountId2, BigDecimal.valueOf(30)));
        assertEquals(1, userAccountEvents.size());

        OwingToUpdatedEvent owingToUpdatedEvent = UserAccount.filterEvent(userAccountEvents, OwingToUpdatedEvent.class);
        assertEquals(accountId1, owingToUpdatedEvent.getAccountId());
        assertEquals(accountId2, owingToUpdatedEvent.getOtherAccountId());
        assertEquals(BigDecimal.valueOf(10).setScale(2), owingToUpdatedEvent.getAmount());
    }


    @Test(expected = UserAccountInvalidStateException.class)
    public void handlePaybackDebitEventWithInvalidState() throws UserAccountInvalidStateException {
        userAccount1.setBalance(BigDecimal.valueOf(30));
        userAccount1.setPendingDebits(accountId2, BigDecimal.valueOf(40));

        // throw invalid exception when balance is less than payout instruction
        userAccount1.handlePaybackDebitEvent(new PaybackDebitEvent(accountId1, accountId2, BigDecimal.valueOf(40)));
    }

    @Test(expected = UserAccountInvalidStateException.class)
    public void handlePaybackDebitEventWithInvalidStatePayingMoreThanOwed() throws UserAccountInvalidStateException {
        userAccount1.setBalance(BigDecimal.valueOf(30));
        userAccount1.setPendingDebits(accountId2, BigDecimal.valueOf(10));

        // throw invalid exception when balance is less than payout instruction
        userAccount1.handlePaybackDebitEvent(new PaybackDebitEvent(accountId1, accountId2, BigDecimal.valueOf(20)));
    }

    @Test
    public void handlePaybackCreditEvent() throws UserAccountInvalidStateException {
        userAccount1.setBalance(BigDecimal.valueOf(50));
        userAccount1.setPendingCredits(accountId2, BigDecimal.valueOf(40));

        Set<UserAccountEvent> userAccountEvents = userAccount1.handlePaybackCreditEvent(new PaybackCreditEvent(accountId1, accountId2, BigDecimal.valueOf(40)));
        // no change in the balance
        assertEquals(BigDecimal.valueOf(50), userAccount1.getBalance());
        assertEquals(1, userAccountEvents.size());

        OwingFromUpdatedEvent owingFromUpdatedEvent  = UserAccount.filterEvent(userAccountEvents, OwingFromUpdatedEvent.class);
        assertEquals(accountId1, owingFromUpdatedEvent.getAccountId());
        assertEquals(accountId2, owingFromUpdatedEvent.getOtherAccountId());
        assertEquals(BigDecimal.ZERO.setScale(2), owingFromUpdatedEvent.getAmount());
    }

    @Test(expected = UserAccountInvalidStateException.class)
    public void handlePaybackCreditEventWithInvalidState() throws UserAccountInvalidStateException {
        userAccount1.setBalance(BigDecimal.valueOf(30));
        userAccount1.setPendingCredits(accountId2, BigDecimal.valueOf(40));

        // throw invalid exception when payback amount received is more than owing records
        userAccount1.handlePaybackDebitEvent(new PaybackDebitEvent(accountId1, accountId2, BigDecimal.valueOf(50)));
    }

    @Test
    public void testRecovered() {
        UserAccount userAccount = new UserAccount(accountId1, userAccountEventSource);
        userAccount.forceRecovery();
        assertFalse(userAccount.isRecovered());
        userAccount.recoverBalance();
        assertTrue(userAccount.isRecovered());
    }


    @Test
    public void testFailedRecovered() {
        doThrow(new IllegalStateException()).when(this.userAccountEventSource).eventSource(any());

        try {
            UserAccount userAccount = new UserAccount(accountId1, userAccountEventSource);
            fail("shoudl have failed recovery");
        } catch(BankOperationException be) {
            assertNotNull(be.getCause());
        }

    }

    @Test(expected = BankOperationException.class)
    public void failedTransfer() {
        when(this.userAccountEventSource.save(any())).thenReturn(false);
        TransferInstruction transferInstruction = new TransferInstruction(accountId1, accountId2, BigDecimal.valueOf(100));
        userAccount1.transfer(transferInstruction);
    }

    @Test
    public void failedTransferEvents() {
        when(this.userAccountEventSource.save(any())).thenReturn(false);
        TransferInstruction transferInstruction = new TransferInstruction(accountId1, accountId2, BigDecimal.valueOf(100));
        try {
            userAccount1.transfer(transferInstruction);
            fail("Should have receveid exception on transfer");
        } catch(BankOperationException e) {
            assertTrue(e.getEvents().isEmpty());
            assertNotNull(e.getCause());
        }
    }


    @Test(expected = BankOperationException.class)
    public void failedTopup() {
        when(this.userAccountEventSource.save(any())).thenReturn(false);
        TopUpAccount topUpAccount = new TopUpAccount(accountId1, BigDecimal.valueOf(100));
        userAccount1.topUp(topUpAccount);
    }

    @Test
    public void failedTopupEvents() {
        when(this.userAccountEventSource.save(any())).thenReturn(false);
        TopUpAccount topUpAccount = new TopUpAccount(accountId1, BigDecimal.valueOf(100));

        try {
            userAccount1.topUp(topUpAccount);
            fail("Should have receveid exception on topup");
        } catch(BankOperationException e) {
            assertTrue(e.getEvents().isEmpty());
            assertNotNull(e.getCause());
        }
    }

    @Test(expected = BankOperationException.class)
    public void failedPayback() {
        when(this.userAccountEventSource.save(any())).thenReturn(false);
        PayoutDebtsInstruction payoutDebtsInstruction = new PayoutDebtsInstruction(accountId1, accountId2, BigDecimal.valueOf(50));
        userAccount1.payback(payoutDebtsInstruction);
    }

    @Test
    public void failedPaybackEvents() {
        when(this.userAccountEventSource.save(any())).thenReturn(false);
        PayoutDebtsInstruction payoutDebtsInstruction = new PayoutDebtsInstruction(accountId1, accountId2, BigDecimal.valueOf(50));
        try {
            userAccount1.payback(payoutDebtsInstruction);
            fail("Should have receveid exception on payback");
        } catch(BankOperationException e) {
            assertTrue(e.getEvents().isEmpty());
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void getStatus() {
        userAccount1.setBalance(BigDecimal.valueOf(50));
        userAccount1.setPendingCredits(accountId2, BigDecimal.valueOf(40));
        userAccount1.setPendingDebits(accountId3, BigDecimal.valueOf(50));

        Set<UserAccountEvent> status = userAccount1.getStatus();
        assertEquals(3, status.size());
        BalanceUpdatedEvent balanceUpdatedEvent = UserAccount.filterOptionalEvent(status, BalanceUpdatedEvent.class).get();
        OwingFromUpdatedEvent owingFromUpdatedEvent = UserAccount.filterOptionalEvent(status, OwingFromUpdatedEvent.class).get();
        OwingToUpdatedEvent owingToUpdatedEvent = UserAccount.filterOptionalEvent(status, OwingToUpdatedEvent.class).get();


        assertTrue(BalanceAmountMath.equals(BigDecimal.valueOf(50), balanceUpdatedEvent.getAmount()));
        assertTrue(BalanceAmountMath.equals( BigDecimal.valueOf(40), owingFromUpdatedEvent.getAmount()));
        assertTrue(BalanceAmountMath.equals(BigDecimal.valueOf(50), owingToUpdatedEvent.getAmount()));

    }
}