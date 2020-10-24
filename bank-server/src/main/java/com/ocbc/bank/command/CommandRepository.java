package com.ocbc.bank.command;

import com.ocbc.bank.command.model.IBankingInstruction;
import com.ocbc.bank.dto.CommandType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CommandRepository {
    private EnumMap<CommandType, IBankingCommand> commandEnumMap;

    public CommandRepository(List<IBankingCommand> iBankingCommands) {
        Map<CommandType, IBankingCommand> collect = iBankingCommands.stream().collect(Collectors.toMap(IBankingCommand::getType, Function.identity()));
        commandEnumMap = new EnumMap(CommandType.class);
        commandEnumMap.putAll(collect);
    }

    public <T extends IBankingInstruction> IBankingCommand<T> getCommand(CommandType commandType) {
        return commandEnumMap.get(commandType);
    }
}
