package com.ocbc.bank.client.matchers.config;

import com.ocbc.bank.client.matchers.LoginInputMatcher;
import com.ocbc.bank.client.matchers.TopUpCommandInputMatcher;
import com.ocbc.bank.client.matchers.TransferCommandInputMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MatchersConfig {

    @Bean
    public TransferCommandInputMatcher transferCommandInputMatcher() {
        return new TransferCommandInputMatcher();
    }

    @Bean
    public TopUpCommandInputMatcher topUpRequestInputParser() {
        return new TopUpCommandInputMatcher();
    }

    @Bean
    public LoginInputMatcher loginInputMatcher() {
        return new LoginInputMatcher();
    }
}
