package com.ocbc.bank.controller;

import com.ocbc.bank.command.CommandRepository;
import com.ocbc.bank.command.IBankingCommand;
import com.ocbc.bank.command.model.LoginForAccount;
import com.ocbc.bank.command.model.TopUpAccount;
import com.ocbc.bank.command.model.TransferInstruction;
import com.ocbc.bank.dto.BankCommandResult;
import com.ocbc.bank.dto.BankOperationResponse;
import com.ocbc.bank.dto.CommandType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * this is front controller for all banking clients.
 * Accepted list of commands are help in {@link CommandRepository}
 */
@RestController
@Slf4j
public class BankServerController {

    private CommandRepository commandRepository;

    public BankServerController(CommandRepository commandRepository) {
        this.commandRepository = commandRepository;
    }

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }


    @PostMapping("/login/{accountId}")
    public BankOperationResponse transfer(@PathVariable String accountId) {
        LoginForAccount instruction = new LoginForAccount(accountId);
        log.info("received login request {}", instruction);
        IBankingCommand<LoginForAccount> command = commandRepository.getCommand(CommandType.LOGIN);
        BankCommandResult execute = command.execute(instruction);
        BankOperationResponse bankOperationResponse = execute.toResponse(accountId, command.getType());
        if(!bankOperationResponse.isSuccess()) {
            log.info("unsuccessful response {} in instruction {}");
        }
        return bankOperationResponse;
    }

    @PostMapping("/transfer/{fromAccountId}/{toAccountId}/{amount}")
    // TODO : get unique request id from the user and handle idempotency
    public BankOperationResponse transfer(@PathVariable String fromAccountId, @PathVariable String toAccountId,
                                          @PathVariable BigDecimal amount) {
        TransferInstruction instruction = new TransferInstruction(fromAccountId, toAccountId, amount);
        log.info("received transfer request {}", instruction);
        IBankingCommand<TransferInstruction> command = commandRepository.getCommand(CommandType.TRANSFER);
        BankCommandResult execute = command.execute(instruction);
        BankOperationResponse bankOperationResponse = execute.toResponse(fromAccountId, command.getType());
        if(!bankOperationResponse.isSuccess()) {
            log.info("unsuccessful response {} in instruction {}");
        }
        return bankOperationResponse;
    }

    @PostMapping("/topUp/{fromAccountId}/{amount}")
    // TODO : get unique request id from the user and handle idempotency
    public BankOperationResponse topup(@PathVariable String fromAccountId, @PathVariable BigDecimal amount) {
        TopUpAccount instruction = new TopUpAccount(fromAccountId, amount);
        log.info("received topup request {}", instruction);
        IBankingCommand<TopUpAccount> command = commandRepository.getCommand(CommandType.TOPUP);
        BankCommandResult execute = command.execute(instruction);
        BankOperationResponse bankOperationResponse = execute.toResponse(fromAccountId, command.getType());
        if(!bankOperationResponse.isSuccess()) {
            log.info("unsuccessful response {} in instruction {}");
        }
        return bankOperationResponse;
    }
}
