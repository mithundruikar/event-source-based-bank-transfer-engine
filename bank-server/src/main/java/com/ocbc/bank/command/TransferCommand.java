package com.ocbc.bank.command;

import com.ocbc.bank.command.dispatcher.AsyncEventDispatcher;
import com.ocbc.bank.command.dispatcher.CreditSideTransferEventsHandler;
import com.ocbc.bank.command.model.TransferInstruction;
import com.ocbc.bank.domain.UserAccount;
import com.ocbc.bank.domain.UserAccountRepository;
import com.ocbc.bank.dto.BankCommandResult;
import com.ocbc.bank.dto.CommandType;
import com.ocbc.bank.events.*;
import com.ocbc.bank.exceptions.BankOperationException;

import java.util.*;

public class TransferCommand implements IBankingCommand<TransferInstruction> {
    private UserAccountRepository userAccountRepository;
    private CreditSideTransferEventsHandler creditSideTransferEventsHandler;
    private AsyncEventDispatcher asyncEventDispatcher;

    public TransferCommand(UserAccountRepository userAccountRepository,
                           CreditSideTransferEventsHandler creditSideTransferEventsHandler,
                           AsyncEventDispatcher asyncEventDispatcher) {
        this.userAccountRepository = userAccountRepository;
        this.creditSideTransferEventsHandler = creditSideTransferEventsHandler;
        this.asyncEventDispatcher = asyncEventDispatcher;
    }

    @Override
    public BankCommandResult execute(TransferInstruction transferInstruction) {
        UserAccount fromAccountBean = userAccountRepository.getUserAccount(transferInstruction.getFromAccountId());
        UserAccount toAccountBean = userAccountRepository.getUserAccount(transferInstruction.getToAccountId());
        try {
            Set<UserAccountEvent> events = transferAtoB(transferInstruction, fromAccountBean, toAccountBean);
            return new BankCommandResult(new ArrayList(events));
        } catch(BankOperationException e) {
            return new BankCommandResult("Error while transferring "+transferInstruction, e.getEvents());
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.TRANSFER;
    }

    private Set<UserAccountEvent> transferAtoB(TransferInstruction transferInstruction, UserAccount fromAccountBean, UserAccount toAccountBean) {
        Set<UserAccountEvent> producedEvents = new LinkedHashSet<>();

        Set<UserAccountEvent> transferEvents = new LinkedHashSet<>();
        fromAccountBean.getReentrantLock().lock();
        try {
            transferEvents.addAll(fromAccountBean.transfer(transferInstruction));
            ActualDebitedEvent actualDebitedEvent = UserAccount.filterEvent(transferEvents, ActualDebitedEvent.class);
            producedEvents.add(actualDebitedEvent);
            producedEvents.addAll(fromAccountBean.handleAccountDebitedEvent(actualDebitedEvent));

            Optional<PendingDebitedEvent> pendingDebitedEvent = UserAccount.filterOptionalEvent(transferEvents, PendingDebitedEvent.class);
            if(pendingDebitedEvent.isPresent()) {
                producedEvents.add(pendingDebitedEvent.get());
                producedEvents.addAll(fromAccountBean.handlePendingDebitedEvent(pendingDebitedEvent.get()));
            }
        } catch(Throwable throwable) {
            fromAccountBean.forceRecovery();
            toAccountBean.forceRecovery();
            throw new BankOperationException(transferInstruction, new ArrayList(producedEvents), throwable);
        }
        finally {
            fromAccountBean.getReentrantLock().unlock();
        }
        asyncEventDispatcher.dispatch(() -> creditSideTransferEventsHandler.handleCreditEvents(toAccountBean.getAccountId(), transferEvents));
        return producedEvents;
    }

}
