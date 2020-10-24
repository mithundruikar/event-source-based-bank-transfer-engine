package com.ocbc.bank.domain;

import com.google.common.annotations.VisibleForTesting;
import com.ocbc.bank.command.model.*;
import com.ocbc.bank.domain.util.BalanceAmountMath;
import com.ocbc.bank.events.*;
import com.ocbc.bank.eventsource.UserAccountEventSource;
import com.ocbc.bank.exceptions.BankOperationException;
import com.ocbc.bank.exceptions.UserAccountInvalidStateException;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Main domain aggregate of the banking server.
 * It accepts {@link com.ocbc.bank.command.IBankingCommand} and generates {@link UserAccountEvent} <br>
 * It follows event sourcing design principle and can be fully recovered using {@link UserAccountEventSource}. See {@link UserAccount#recoverBalance()} method.
 *
 * This domain needs to be used via appropriate command {@link com.ocbc.bank.command.IBankingCommand} <br>
 * All event handler method changes the state of the domain. Hence permit needs to held by the command before using the domain. See {@link UserAccount#getReentrantLock()}
 *
 */
public class UserAccount {
    private final String accountId;
    /**
     * This balance being 0 does not necessarily represent account balance is 0.
     * It should be treated only considering value of {@link UserAccount#recovered} flag
     */
    private BigDecimal balance = BalanceAmountMath.get(BigDecimal.ZERO);
    /**
     * This flag indicates whether user account domain is ready to be used
     * Only when its status is recovered from the transaction logs, domain object should be handling commands
     */
    private volatile boolean recovered = false;
    private Map<String, BigDecimal> pendingDebits;
    private Map<String, BigDecimal> pendingCredits;
    private UserAccountEventSource userAccountEventSource;
    private final ReentrantLock reentrantLock;

    public UserAccount(String accountId, UserAccountEventSource userAccountEventSource) {
        this.accountId = accountId;
        this.pendingDebits = new LinkedHashMap<>();
        this.pendingCredits = new LinkedHashMap<>();
        this.reentrantLock = new ReentrantLock();
        this.userAccountEventSource = userAccountEventSource;
        this.recovered = false;
        recoverBalance();
    }

    public Set<UserAccountEvent> transfer(TransferInstruction transferInstruction) {
        recoverIfRequired();

        Set<UserAccountEvent> events = new LinkedHashSet<>();

        if(BalanceAmountMath.compareTo(transferInstruction.getAmount(), this.balance) > 0) {
            // pending payments involved
            BigDecimal pendingAmount = BalanceAmountMath.subtract(transferInstruction.getAmount(), this.balance);
            events.add(new PendingDebitedEvent(transferInstruction.getFromAccountId(),
                    transferInstruction.getToAccountId(), pendingAmount));

            events.add(new PendingCreditedEvent(transferInstruction.getToAccountId(),
                    transferInstruction.getFromAccountId(),
                    pendingAmount));
        }
        BigDecimal possibleTransferAmount = BalanceAmountMath.getPossibleTransferAmount(transferInstruction.getAmount(), this.balance);
        events.add(new ActualDebitedEvent(transferInstruction.getFromAccountId(),
                transferInstruction.getToAccountId(), possibleTransferAmount));

        events.add(new ActualCreditedEvent(transferInstruction.getToAccountId(),
                transferInstruction.getFromAccountId(),
                possibleTransferAmount));

        if(!userAccountEventSource.save(events)) {
            throw new BankOperationException(transferInstruction, new IllegalStateException("Not able to persist command state"));
        }

        return events;
    }

    public Set<UserAccountEvent> payback(PayoutDebtsInstruction payoutDebtsInstruction) {
        recoverIfRequired();

        Set<UserAccountEvent> events = new LinkedHashSet<>();

        events.add(new PaybackDebitEvent(payoutDebtsInstruction.getAccountId(),
                payoutDebtsInstruction.getToAccountId(), payoutDebtsInstruction.getAmount()));

        events.add(new PaybackCreditEvent(payoutDebtsInstruction.getToAccountId(),
                payoutDebtsInstruction.getAccountId(), payoutDebtsInstruction.getAmount()));

        events.add(new ActualDebitedEvent(payoutDebtsInstruction.getAccountId(),
                payoutDebtsInstruction.getToAccountId(), payoutDebtsInstruction.getAmount()));

        events.add(new ActualCreditedEvent(payoutDebtsInstruction.getToAccountId(),
                payoutDebtsInstruction.getAccountId(), payoutDebtsInstruction.getAmount()));

        if(!userAccountEventSource.save(events)) {
            throw new BankOperationException(payoutDebtsInstruction, new IllegalStateException("Not able to persist command state"));
        }

        return events;
    }

    public TopUpCompletedEvent topUp(TopUpAccount topUpAccount) {
        recoverIfRequired();

        TopUpCompletedEvent topUpCompletedEvent = new TopUpCompletedEvent(accountId, topUpAccount.getAmount());
        if(!userAccountEventSource.save(Collections.singletonList(topUpCompletedEvent))) {
            throw new BankOperationException(topUpAccount, new IllegalStateException("Not able to persist command state"));
        }
        return topUpCompletedEvent;
    }

    public Set<UserAccountEvent> recoverBalance() {
        this.recovered = false;

        try {
            this.userAccountEventSource.eventSource(this);
        } catch (RuntimeException re) {
            throw new BankOperationException(Collections.emptyList(), new IllegalStateException("Not able to recover state of account "+accountId));
        }
        Set<UserAccountEvent> events = getStatus();
        this.recovered = true;
        return events;
    }

    public Set<UserAccountEvent> getStatus() {
        Set<UserAccountEvent> events = new LinkedHashSet<>();
        events.add(getBalanceUpdatedEvent(this.balance));
        events.addAll(getAllOwingToRecordEvents());
        events.addAll(getAllOwingFromRecordEvents());
        return events;
    }

    public Set<UserAccountEvent> recoverIfRequired() {
        if(!this.recovered) {
            return recoverBalance();
        }
        return Collections.emptySet();
    }

    public boolean isRecovered() {
        return recovered;
    }

    public void forceRecovery() {
        this.recovered = false;
    }

    public DomainOperationResult handleAccountCreditedEvent(ActualCreditedEvent actualCreditedEvent) {

        Set<UserAccountEvent> events = new LinkedHashSet<>(1);

        BigDecimal updatedBalance = BalanceAmountMath.add(balance, actualCreditedEvent.getAmount());
        BalanceUpdatedEvent balanceUpdatedEvent = updateBalance(updatedBalance);
        events.add(balanceUpdatedEvent);

        return new DomainOperationResult(events, getPayoutDebtsInstructions());
    }

    public Set<UserAccountEvent> handleAccountDebitedEvent(ActualDebitedEvent actualDebitedEvent) {

        Set<UserAccountEvent> events = new LinkedHashSet<>(1);
        events.add(updateBalance(balance.subtract(actualDebitedEvent.getAmount())));
        return events;
    }

    public Set<UserAccountEvent> handlePendingDebitedEvent(PendingDebitedEvent pendingDebitedEvent) {
        Set<UserAccountEvent> events = new LinkedHashSet<>(1);

        OwingToUpdatedEvent owingToUpdatedEvent = updateOwingToRecords(pendingDebitedEvent.getOtherAccountId(), pendingDebitedEvent.getAmount());
        events.add(owingToUpdatedEvent);

        return events;
    }

    public Set<UserAccountEvent> handlePendingCreditedEvent(PendingCreditedEvent pendingCreditedEvent) {
        Set<UserAccountEvent> events = new LinkedHashSet<>(1);

        OwingFromUpdatedEvent owingToUpdatedEvent = updateOwingFromRecords(pendingCreditedEvent.getOtherAccountId(), pendingCreditedEvent.getAmount());
        events.add(owingToUpdatedEvent);

        return events;
    }


    public DomainOperationResult handleTopUpCompletedEvent(TopUpCompletedEvent topUpCompletedEvent) {
        return handleAccountCreditedEvent(topUpCompletedEvent);
    }

    public Set<UserAccountEvent> handlePaybackDebitEvent(PaybackDebitEvent paybackDebitEvent) throws UserAccountInvalidStateException  {
        Set<UserAccountEvent> events = new LinkedHashSet<>();
        if(BalanceAmountMath.compareTo(this.balance, paybackDebitEvent.getAmount()) < 0) {
            throw new UserAccountInvalidStateException(this.accountId, "trying to payback more than then available balance. balance "+this.balance+" event: "+paybackDebitEvent);
        }
        BigDecimal knownOwedAmount = this.pendingDebits.get(paybackDebitEvent.getOtherAccountId());
        if(Objects.isNull(knownOwedAmount) || BalanceAmountMath.compareTo(knownOwedAmount, paybackDebitEvent.getAmount()) < 0) {
            throw new UserAccountInvalidStateException(this.accountId, "trying to payback more than then known owed balance. knownOwedAmount "+knownOwedAmount+" event: "+paybackDebitEvent);
        }

        BigDecimal updatedOwedAmount = BalanceAmountMath.subtract(knownOwedAmount, paybackDebitEvent.getAmount());
        if(BalanceAmountMath.equals(updatedOwedAmount, BigDecimal.ZERO)) {
            this.pendingDebits.remove(paybackDebitEvent.getOtherAccountId());
        } else {
            this.pendingDebits.put(paybackDebitEvent.getOtherAccountId(), updatedOwedAmount);
        }
        events.add(new OwingToUpdatedEvent(accountId, paybackDebitEvent.getOtherAccountId(), updatedOwedAmount));
        return events;
    }

    public Set<UserAccountEvent> handlePaybackCreditEvent(PaybackCreditEvent paybackCreditEvent) throws UserAccountInvalidStateException  {
        Set<UserAccountEvent> events = new LinkedHashSet<>();
        BigDecimal knownOwedAmount = this.pendingCredits.get(paybackCreditEvent.getOtherAccountId());
        if(Objects.isNull(knownOwedAmount) || BalanceAmountMath.compareTo(knownOwedAmount, paybackCreditEvent.getAmount()) < 0) {
            throw new UserAccountInvalidStateException(this.accountId, "trying to payback more than then known owed balance. knownOwedAmount "+knownOwedAmount+" event: "+paybackCreditEvent);
        }

        BigDecimal updatedOwedAmount = BalanceAmountMath.subtract(knownOwedAmount, paybackCreditEvent.getAmount());
        if(BalanceAmountMath.equals(updatedOwedAmount, BigDecimal.ZERO)) {
            this.pendingCredits.remove(paybackCreditEvent.getOtherAccountId());
        } else {
            this.pendingCredits.put(paybackCreditEvent.getOtherAccountId(), updatedOwedAmount);
        }
        events.add(new OwingFromUpdatedEvent(accountId, paybackCreditEvent.getOtherAccountId(), updatedOwedAmount));
        return events;
    }

    public ReentrantLock getReentrantLock() {
        return reentrantLock;
    }

    private Set<PayoutDebtsInstruction> getPayoutDebtsInstructions() {
        Set<PayoutDebtsInstruction> instructions = new LinkedHashSet<>();
        BigDecimal availableBalance = this.balance;
        for (String toAccount : this.pendingDebits.keySet()) {
            if(BalanceAmountMath.equals(availableBalance, BigDecimal.ZERO)) {
                return instructions;
            }
            BigDecimal possibleTransferAmount = BalanceAmountMath.getPossibleTransferAmount(this.pendingDebits.get(toAccount), availableBalance);
            instructions.add(new PayoutDebtsInstruction(this.accountId, toAccount, possibleTransferAmount));
            availableBalance = BalanceAmountMath.subtract(availableBalance, possibleTransferAmount);
        }
        return instructions;
    }

    public static <T extends UserAccountEvent> T filterEvent(Set<UserAccountEvent> userAccountEvents, Class<T> tClass) {
        List<T> collect = userAccountEvents.stream()
                .filter(event -> tClass.isInstance(event))
                .map(event -> (T) event)
                .collect(Collectors.toList());
        if(collect.size() != 1) {
            throw new IllegalStateException(String.format("Expected 1 but found more than 1 event of type %s found %s", tClass.getSimpleName(), collect.size()));
        }
        return collect.get(0);
    }

    public static <T extends UserAccountEvent> Optional<T> filterOptionalEvent(Set<UserAccountEvent> userAccountEvents, Class<T> tClass) {
        List<T> collect = userAccountEvents.stream()
                .filter(event -> tClass.isInstance(event))
                .map(event -> (T) event)
                .collect(Collectors.toList());
        if(collect.isEmpty()) {
            return Optional.empty();
        }
        if(collect.size() != 1) {
            throw new IllegalStateException(String.format("Expected 1 but found more than 1 event of type %s found %s", tClass.getSimpleName(), collect.size()));
        }
        return Optional.of(collect.get(0));
    }

    public static <T extends UserAccountEvent> Optional<T> filterOptionalEvent(UserAccountEvent userAccountEvent, Class<T> tClass) {
        return tClass.equals(userAccountEvent.getClass()) ? Optional.of((T) userAccountEvent) : Optional.empty();
    }

    public static <T extends UserAccountEvent> List<T> filterEvents(Collection<UserAccountEvent> userAccountEvents, Class<T> tClass) {
        return userAccountEvents.stream()
                .filter(event -> tClass.isInstance(event))
                .map(event -> (T) event)
                .collect(Collectors.toList());
    }

    public static <T extends IBankingInstruction> List<T> filterInstructions(Set<T> userAccountEvents, Class<T> tClass) {
        return userAccountEvents.stream()
                .filter(event -> tClass.isInstance(event))
                .map(event -> (T) event)
                .collect(Collectors.toList());
    }


    private OwingToUpdatedEvent updateOwingToRecords(String toAccountId, BigDecimal owingUpdate) {
        pendingDebits.compute(toAccountId, (k, v) -> v == null ? owingUpdate : BalanceAmountMath.add(v, owingUpdate));
        return new OwingToUpdatedEvent(this.accountId, toAccountId, pendingDebits.get(toAccountId));
    }

    private OwingFromUpdatedEvent updateOwingFromRecords(String fromAccountId, BigDecimal owingUpdate) {
        pendingCredits.compute(fromAccountId, (k, v) -> v == null ? owingUpdate : BalanceAmountMath.add(v, owingUpdate));
        return new OwingFromUpdatedEvent(this.accountId, fromAccountId, pendingCredits.get(fromAccountId));
    }

    private Set<OwingToUpdatedEvent> getAllOwingToRecordEvents() {
        return new LinkedHashSet(pendingDebits.entrySet().stream()
                .filter(entry -> !BalanceAmountMath.equals(entry.getValue(), BigDecimal.ZERO))
                .map(entry -> new OwingToUpdatedEvent(this.accountId, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
    }

    private Set<OwingFromUpdatedEvent> getAllOwingFromRecordEvents() {
        return new LinkedHashSet(pendingCredits.entrySet().stream()
                .filter(entry -> !BalanceAmountMath.equals(entry.getValue(), BigDecimal.ZERO))
                .map(entry -> new OwingFromUpdatedEvent(this.accountId, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
    }

    private BalanceUpdatedEvent updateBalance(BigDecimal updatedBalance) {
        BigDecimal currentBalance = balance;
        balance = updatedBalance;
        return getBalanceUpdatedEvent(currentBalance);
    }

    private BalanceUpdatedEvent getBalanceUpdatedEvent(BigDecimal previousBalance) {
        return new BalanceUpdatedEvent(this.accountId, previousBalance, balance);
    }

    @VisibleForTesting
    protected void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getAccountId() {
        return accountId;
    }

    @VisibleForTesting
    protected void setPendingDebits(String toAccount, BigDecimal amount) {
        this.pendingDebits.put(toAccount, amount);
    }

    @VisibleForTesting
    protected void setPendingCredits(String toAccount, BigDecimal amount) {
        this.pendingCredits.put(toAccount, amount);
    }

    @VisibleForTesting
    void setRecovered() {
        this.recovered = true;
    }


}
