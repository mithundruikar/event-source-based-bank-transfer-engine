package com.ocbc.bank.client.processor;

import com.ocbc.bank.client.connector.HttpConnector;
import com.ocbc.bank.client.printer.BankOperationResponsePrinter;
import com.ocbc.bank.client.session.UserSessionManager;
import com.ocbc.bank.dto.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BankServerInvokerTest {
    @Mock
    private HttpConnector httpConnector;

    private BankServerInvoker bankServerInvoker;
    private UserSessionManager userSessionManager;

    @Before
    public void setup() {
        this.userSessionManager = new UserSessionManager();
        this.bankServerInvoker = new BankServerInvoker(this.userSessionManager, httpConnector, new BankOperationResponsePrinter());
    }

    @Test
    public void getResponseForLoginRequest() {
        BankOperationRequest bankOperationRequest = new BankOperationRequest("A", CommandType.LOGIN, null, null);
        BankOperationResult bankOperationResult = new BankOperationResult(EventType.BALANCE_UPDATE.name(), null, BigDecimal.valueOf(100l));
        BankOperationResponse bankOperationResponse = new BankOperationResponse("A", true, null, CommandType.LOGIN, Arrays.asList(bankOperationResult));
        when(httpConnector.getResponse(eq("login/A"))).thenReturn(bankOperationResponse);

        List<String> response = this.bankServerInvoker.getResponse(bankOperationRequest);

        assertTrue(response.contains("your balance is 100"));
    }

    @Test
    public void getResponseForPayment() {
        BankOperationRequest bankOperationRequest = new BankOperationRequest("A", CommandType.TRANSFER, "B", BigDecimal.valueOf(100l));
        BankOperationResult bankOperationResult = new BankOperationResult(EventType.CNF_DEBIT.name(), "B", BigDecimal.valueOf(100l));
        BankOperationResponse bankOperationResponse = new BankOperationResponse("A", true, null, CommandType.TRANSFER, Arrays.asList(bankOperationResult));
        when(httpConnector.getResponse(eq("transfer/A/B/100"))).thenReturn(bankOperationResponse);

        List<String> response = this.bankServerInvoker.getResponse(bankOperationRequest);

        assertTrue(response.contains("debited 100 from A"));
    }

    @Test
    public void getResponseForTopUp() {
        BankOperationRequest bankOperationRequest = new BankOperationRequest("A", CommandType.TOPUP, null, BigDecimal.valueOf(100l));
        BankOperationResult bankOperationResult = new BankOperationResult(EventType.CNF_PAYBACK_DEBIT.name(), "B", BigDecimal.valueOf(100l));
        BankOperationResponse bankOperationResponse = new BankOperationResponse("A", true, null, CommandType.TOPUP, Arrays.asList(bankOperationResult));
        when(httpConnector.getResponse(eq("topUp/A/100"))).thenReturn(bankOperationResponse);

        List<String> response = this.bankServerInvoker.getResponse(bankOperationRequest);

        assertTrue(response.contains("Transferred 100 to B"));
    }
}