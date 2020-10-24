package com.ocbc.bank.client.processor.config;

import com.ocbc.bank.client.connector.HttpConnector;
import com.ocbc.bank.client.printer.BankOperationResponsePrinter;
import com.ocbc.bank.client.processor.BankServerInvoker;
import com.ocbc.bank.client.session.UserSessionManager;
import com.ocbc.bank.client.session.config.UserSessionConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(UserSessionConfig.class)
public class ClientProcessorConfig {

    @Bean
    public HttpConnector httpConnector() {
        return new HttpConnector();
    }

    @Bean
    public BankOperationResponsePrinter bankOperationResponsePrinter() {
        return new BankOperationResponsePrinter();
    }

    @Bean
    public BankServerInvoker bankServerInvoker(UserSessionManager userSessionManager, HttpConnector httpConnector, BankOperationResponsePrinter bankOperationResponsePrinter) {
        return new BankServerInvoker(userSessionManager, httpConnector, bankOperationResponsePrinter);
    }
}
