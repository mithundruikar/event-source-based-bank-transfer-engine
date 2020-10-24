package com.ocbc.bank.config;

import com.ocbc.bank.command.*;
import com.ocbc.bank.command.dispatcher.AsyncEventDispatcher;
import com.ocbc.bank.command.dispatcher.CreditSideTransferEventsHandler;
import com.ocbc.bank.domain.UserAccountRepository;
import com.ocbc.bank.dto.TransactionLogRepository;
import com.ocbc.bank.eventsource.EventToDtoMapper;
import com.ocbc.bank.eventsource.UserAccountEventSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CommandsConfig {

    @Bean
    public UserAccountRepository userAccountRepository(UserAccountEventSource userAccountEventSource) {
        return new UserAccountRepository(userAccountEventSource);
    }

    @Bean
    public UserAccountEventSource userAccountEventSource(TransactionLogRepository transactionLogRepository) {
        return new UserAccountEventSource(transactionLogRepository, new EventToDtoMapper());
    }

    @Bean
    public AsyncEventDispatcher asyncEventDispatcher() {
        return new AsyncEventDispatcher();
    }

    @Bean
    public CommandRepository commandRepository(List<IBankingCommand> commands) {
        return new CommandRepository(commands);
    }

    @Bean
    public CreditSideTransferEventsHandler creditSideTransferEventDispatcher1(UserAccountRepository userAccountRepository,
                                                                              AsyncEventDispatcher asyncEventDispatcher) {
        return new CreditSideTransferEventsHandler(userAccountRepository, asyncEventDispatcher);
    }

    @Bean
    public TransferCommand transferCommand(UserAccountRepository userAccountRepository,
                                           CreditSideTransferEventsHandler creditSideTransferEventsHandler,
                                           AsyncEventDispatcher asyncEventDispatcher) {
        return new TransferCommand(userAccountRepository, creditSideTransferEventsHandler, asyncEventDispatcher);
    }

    @Bean
    public TopUpCommand topUpCommand(UserAccountRepository userAccountRepository,
                                     CreditSideTransferEventsHandler creditSideTransferEventsHandler) {
        return new TopUpCommand(userAccountRepository, creditSideTransferEventsHandler);
    }

    @Bean
    public LogInCommand logInCommand(UserAccountRepository userAccountRepository) {
        return new LogInCommand(userAccountRepository);
    }

}
