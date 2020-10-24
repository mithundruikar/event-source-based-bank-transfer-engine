package com.ocbc.bank.command.model;

import com.ocbc.bank.domain.util.BalanceAmountMath;

import java.math.BigDecimal;

public class TopUpAccount implements IBankingInstruction {
    private final String accountId;
    private final BigDecimal amount;

    public TopUpAccount(String accountId, BigDecimal amount) {
        this.accountId = accountId;
        this.amount = BalanceAmountMath.get(amount);
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "TopUpAccount{" +
                "accountId='" + accountId + '\'' +
                ", amount=" + amount +
                '}';
    }
}
