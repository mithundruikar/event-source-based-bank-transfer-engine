package com.ocbc.bank.command;

import com.ocbc.bank.command.model.LoginForAccount;
import com.ocbc.bank.domain.UserAccountRepository;
import com.ocbc.bank.dto.BankCommandResult;
import com.ocbc.bank.events.UserAccountEvent;
import com.ocbc.bank.eventsource.UserAccountEventSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LogInCommandTest {
    @Mock
    private UserAccountEventSource userAccountEventSource;

    private LogInCommand logInCommand;

    @Before
    public void setup() {
        UserAccountRepository userAccountRepository = new UserAccountRepository(userAccountEventSource);
        this.logInCommand = new LogInCommand(userAccountRepository);
    }

    @Test
    public void login() {
        LoginForAccount loginForAccount = new LoginForAccount("TEST");
        BankCommandResult execute = this.logInCommand.execute(loginForAccount);
        List<UserAccountEvent> userAccountEvents = execute.getUserAccountEvents();
        assertFalse(execute.getErrorMessage().isPresent());
        assertEquals(1, userAccountEvents.size());
    }

    @Test
    public void loginWithException() {
        doThrow(new IllegalStateException("Failed to load events from log")).when(userAccountEventSource).eventSource(any());

        LoginForAccount loginForAccount = new LoginForAccount("TEST");
        BankCommandResult execute = this.logInCommand.execute(loginForAccount);
        assertFalse(execute.isSuccess());
        assertTrue(execute.getUserAccountEvents().isEmpty());
        assertTrue(execute.getErrorMessage().isPresent());

    }

}