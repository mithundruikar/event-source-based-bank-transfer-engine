package com.ocbc.bank.command;

import com.ocbc.bank.command.dispatcher.CreditSideTransferEventsHandler;
import com.ocbc.bank.command.model.TopUpAccount;
import com.ocbc.bank.domain.UserAccount;
import com.ocbc.bank.domain.UserAccountRepository;
import com.ocbc.bank.dto.BankCommandResult;
import com.ocbc.bank.dto.CommandType;
import com.ocbc.bank.events.*;
import com.ocbc.bank.exceptions.BankOperationException;

import java.util.*;

public class TopUpCommand implements IBankingCommand<TopUpAccount> {
    private UserAccountRepository userAccountRepository;
    private CreditSideTransferEventsHandler creditSideTransferEventsHandler;

    public TopUpCommand(UserAccountRepository userAccountRepository, CreditSideTransferEventsHandler creditSideTransferEventsHandler) {
        this.userAccountRepository = userAccountRepository;
        this.creditSideTransferEventsHandler = creditSideTransferEventsHandler;
    }

    @Override
    public BankCommandResult execute(TopUpAccount topUpAccount) {
        UserAccount fromAccountBean = userAccountRepository.getUserAccount(topUpAccount.getAccountId());
        try {
            Set<UserAccountEvent> events = topUp(topUpAccount, fromAccountBean);
            return new BankCommandResult(new ArrayList(events));
        } catch(BankOperationException e) {
            // log error
            return new BankCommandResult("Error while topping up "+topUpAccount, e.getEvents());
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.TOPUP;
    }

    private Set<UserAccountEvent> topUp(TopUpAccount topUpAccount, UserAccount fromAccountBean) {
        Set<UserAccountEvent> producedEvents = new LinkedHashSet<>();
        fromAccountBean.getReentrantLock().lock();
        try {
            TopUpCompletedEvent topUpCompletedEvent = fromAccountBean.topUp(topUpAccount);
            producedEvents.add(topUpCompletedEvent);

            Set<UserAccountEvent> creditEvents = this.creditSideTransferEventsHandler.handleCreditEvents(fromAccountBean.getAccountId(), new HashSet<>(Arrays.asList(topUpCompletedEvent)));
            producedEvents.addAll(creditEvents);
        } catch(Throwable t) {
            fromAccountBean.forceRecovery();
            throw new BankOperationException(topUpAccount, new ArrayList(producedEvents), t);
        } finally {
            fromAccountBean.getReentrantLock().unlock();
        }

        return producedEvents;
    }

}
