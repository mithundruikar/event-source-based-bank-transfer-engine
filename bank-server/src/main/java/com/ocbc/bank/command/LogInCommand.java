package com.ocbc.bank.command;

import com.ocbc.bank.command.model.LoginForAccount;
import com.ocbc.bank.domain.UserAccount;
import com.ocbc.bank.domain.UserAccountRepository;
import com.ocbc.bank.dto.BankCommandResult;
import com.ocbc.bank.dto.CommandType;
import com.ocbc.bank.events.UserAccountEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class LogInCommand implements IBankingCommand<LoginForAccount> {
    private UserAccountRepository userAccountRepository;

    public LogInCommand(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public BankCommandResult execute(LoginForAccount loginForAccount) {
        Set<UserAccountEvent> producedEvents = new LinkedHashSet<>();
        try {
            UserAccount fromAccountBean = userAccountRepository.getUserAccount(loginForAccount.getAccountId());
            fromAccountBean.recoverIfRequired();
            producedEvents.addAll(fromAccountBean.getStatus());
            return new BankCommandResult(new ArrayList(producedEvents));
        } catch(Throwable e) {
            log.error("Error while performing login/recovery for {} events produced {}", loginForAccount.getAccountId(), producedEvents, e);
            return new BankCommandResult("Error while performing login/recovery for "+loginForAccount.getAccountId(), new ArrayList<>(producedEvents));
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.LOGIN;
    }

}
