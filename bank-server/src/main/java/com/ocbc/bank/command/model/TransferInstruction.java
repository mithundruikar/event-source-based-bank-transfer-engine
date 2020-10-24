package com.ocbc.bank.command.model;

import com.ocbc.bank.domain.util.BalanceAmountMath;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@ToString
@EqualsAndHashCode
public final class TransferInstruction implements IBankingInstruction {

    private final String fromAccountId;
    private final String toAccountId;
    private final BigDecimal amount;

    public TransferInstruction(String fromAccountId, String toAccountId, BigDecimal amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = BalanceAmountMath.get(amount);
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
