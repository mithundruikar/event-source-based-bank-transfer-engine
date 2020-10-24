package com.ocbc.bank.client.session.config;

import com.ocbc.bank.client.session.UserSessionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserSessionConfig {

    @Bean
    public UserSessionManager userSessionManager() {
        return new UserSessionManager();
    }
}
