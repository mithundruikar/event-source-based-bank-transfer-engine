package com.ocbc.bank.client.processor;

import com.ocbc.bank.client.connector.HttpConnector;
import com.ocbc.bank.client.printer.BankOperationResponsePrinter;
import com.ocbc.bank.client.session.UserSessionManager;
import com.ocbc.bank.dto.BankOperationRequest;
import com.ocbc.bank.dto.BankOperationResponse;
import com.ocbc.bank.dto.CommandType;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class BankServerInvoker {

    private UserSessionManager userSessionManager;
    private HttpConnector httpConnector;
    private Map<CommandType, Function<BankOperationRequest, String>> resourcePathMapper = new HashMap<>();
    private BankOperationResponsePrinter bankOperationResponsePrinter;

    public BankServerInvoker(UserSessionManager userSessionManager, HttpConnector httpConnector, BankOperationResponsePrinter bankOperationResponsePrinter) {
        this.userSessionManager = userSessionManager;
        this.httpConnector = httpConnector;
        this.bankOperationResponsePrinter = bankOperationResponsePrinter;

        this.resourcePathMapper.put(CommandType.LOGIN, this::getLoginResourcePath);
        this.resourcePathMapper.put(CommandType.TRANSFER, this::getTransferResourcePath);
        this.resourcePathMapper.put(CommandType.TOPUP, this::getTopupResourcePath);
    }

    public List<String> getResponse(BankOperationRequest bankOperationRequest) {
        String resourcePath = this.resourcePathMapper.get(bankOperationRequest.getCommand()).apply(bankOperationRequest);
        BankOperationResponse response = null;
        try {
            response = httpConnector.getResponse(resourcePath);
            if(response == null) {
                return Collections.singletonList("Error occured while performing request. Please try again later");
            }
        } catch (RuntimeException re) {
            log.error("Error while calling server {}", bankOperationRequest, re);
            return Collections.singletonList("Error occured while performing request. Please try again later");
        }

        // TODO - make more framework level handlers rather than such login response handling

        if(!response.isSuccess()) {
            log.warn("unsuccessful command result from server {}", response);
            return Collections.singletonList("Command was not successful. Please try again later or check with support. <there should be some relevant error code here>");
        }

        List<String> print = this.bankOperationResponsePrinter.print(response);
        if(response.isSuccess() && CommandType.LOGIN.equals(bankOperationRequest.getCommand())) {
            userSessionManager.login(bankOperationRequest.getAccountId());
            print.add(0, "Hello, "+bankOperationRequest.getAccountId());
        }
        return print;
    }

    private String getTransferResourcePath(BankOperationRequest bankOperationRequest) {
        return "transfer/"+bankOperationRequest.getAccountId()+"/"+bankOperationRequest.getToAccountId()+"/"+bankOperationRequest.getAmount();
    }

    private String getTopupResourcePath(BankOperationRequest bankOperationRequest) {
        return "topUp/"+bankOperationRequest.getAccountId()+"/"+bankOperationRequest.getAmount();
    }

    private String getLoginResourcePath(BankOperationRequest bankOperationRequest) {
        return "login/"+bankOperationRequest.getAccountId();
    }
}
