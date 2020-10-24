package com.ocbc.bank.client;

import com.ocbc.bank.client.matchers.LoginInputMatcher;
import com.ocbc.bank.client.matchers.TopUpCommandInputMatcher;
import com.ocbc.bank.client.matchers.TransferCommandInputMatcher;
import com.ocbc.bank.client.port.UserInputPort;
import com.ocbc.bank.client.processor.BankServerInvoker;
import com.ocbc.bank.client.session.UserSessionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BankUserInterfaceTest {

    @Mock
    private BankServerInvoker bankServerInvoker;

    @Mock
    private UserInputPort userInputPort;

    private BankUserInterface bankUserInterface;
    private UserSessionManager userSessionManager;

    @Before
    public void setup() {
        this.userSessionManager = new UserSessionManager();
        this.bankUserInterface = new BankUserInterface(userInputPort, Arrays.asList(new LoginInputMatcher(), new TransferCommandInputMatcher(), new TopUpCommandInputMatcher()),
                this.userSessionManager, bankServerInvoker);
    }

    @Test
    public void sendLoginCommand() {
        when(this.userInputPort.getNextLine()).thenReturn("login ABC", "exit");
        List<String> expectedOutputs = Arrays.asList("Hello, ABC", "your balance is 0.0");
        when(bankServerInvoker.getResponse(any())).thenReturn(expectedOutputs);
        this.bankUserInterface.start();

        ArgumentCaptor<String> outputCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.userInputPort, atLeast(1)).writeLine(outputCaptor.capture());

        List<String> allValues = outputCaptor.getAllValues();
        List<String> matchedOutputs = expectedOutputs.stream().filter(allValues::contains).collect(Collectors.toList());
        assertEquals(expectedOutputs, matchedOutputs);
    }


    @Test
    public void sendPayCommand() {
        when(this.userInputPort.getNextLine()).thenReturn("login ABC", "pay DEF 450", "exit");
        List<String> expectedOutputs = Arrays.asList("Hello, ABC", "Transferred 450 to Alice", "your balance is 0.0");
        when(bankServerInvoker.getResponse(any())).thenReturn(expectedOutputs);
        this.bankUserInterface.start();

        ArgumentCaptor<String> outputCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.userInputPort, atLeast(1)).writeLine(outputCaptor.capture());

        List<String> allValues = outputCaptor.getAllValues();
        List<String> matchedOutputs = expectedOutputs.stream().filter(allValues::contains).collect(Collectors.toList());
        assertEquals(expectedOutputs, matchedOutputs);
    }



    @Test
    public void sendTopupCommand() {
        when(this.userInputPort.getNextLine()).thenReturn("login ABC", "topup 450", "exit");
        List<String> expectedOutputs = Arrays.asList("Hello, ABC", "your balance is 450.00");
        when(bankServerInvoker.getResponse(any())).thenReturn(expectedOutputs);
        this.bankUserInterface.start();

        ArgumentCaptor<String> outputCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.userInputPort, atLeast(1)).writeLine(outputCaptor.capture());

        List<String> allValues = outputCaptor.getAllValues();
        List<String> matchedOutputs = expectedOutputs.stream().filter(allValues::contains).collect(Collectors.toList());
        assertEquals(expectedOutputs, matchedOutputs);
    }

    @Test
    public void commandBeforeLogin() {
        when(this.userInputPort.getNextLine()).thenReturn("topup 450", "exit");
        List<String> expectedOutputs = Arrays.asList("please login first before continuing...");
        this.bankUserInterface.start();

        ArgumentCaptor<String> outputCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.userInputPort, atLeast(1)).writeLine(outputCaptor.capture());

        List<String> allValues = outputCaptor.getAllValues();
        List<String> matchedOutputs = expectedOutputs.stream().filter(allValues::contains).collect(Collectors.toList());
        assertEquals(expectedOutputs, matchedOutputs);
    }

}