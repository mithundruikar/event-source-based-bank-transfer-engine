package com.ocbc.bank.command;

import com.ocbc.bank.command.model.IBankingInstruction;
import com.ocbc.bank.dto.BankCommandResult;
import com.ocbc.bank.dto.CommandType;


public interface IBankingCommand<T extends IBankingInstruction> {
    BankCommandResult execute(T instruction);

    CommandType getType();
}
