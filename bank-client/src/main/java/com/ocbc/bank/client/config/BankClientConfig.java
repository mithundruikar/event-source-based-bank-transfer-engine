package com.ocbc.bank.client.config;

import com.ocbc.bank.client.BankUserInterface;
import com.ocbc.bank.client.matchers.UserInputMatcher;
import com.ocbc.bank.client.matchers.config.MatchersConfig;
import com.ocbc.bank.client.port.ConsoleInputPort;
import com.ocbc.bank.client.port.UserInputPort;
import com.ocbc.bank.client.processor.BankServerInvoker;
import com.ocbc.bank.client.processor.config.ClientProcessorConfig;
import com.ocbc.bank.client.session.UserSessionManager;
import com.ocbc.bank.client.session.config.UserSessionConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

@Configuration
@Import({MatchersConfig.class, UserSessionConfig.class, ClientProcessorConfig.class})
public class BankClientConfig {

    @Bean
    public UserInputPort userInputPort() {
        return new ConsoleInputPort();
    }

    @Bean
    public BankUserInterface bankCli(UserInputPort userInputPort, UserSessionManager userSessionManager, List<UserInputMatcher> userInputMatchers, BankServerInvoker bankServerInvoker) {
        return new BankUserInterface(userInputPort, userInputMatchers, userSessionManager, bankServerInvoker);
    }

}
