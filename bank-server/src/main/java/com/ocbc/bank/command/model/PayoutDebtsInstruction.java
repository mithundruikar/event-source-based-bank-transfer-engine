package com.ocbc.bank.command.model;


import lombok.ToString;

import java.math.BigDecimal;
import java.util.Objects;

@ToString
public class PayoutDebtsInstruction implements IBankingInstruction {
    private final String accountId;
    private final String toAccountId;
    private final BigDecimal amount;

    public PayoutDebtsInstruction(String accountId, String toAccountId, BigDecimal amount) {
        this.accountId = accountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PayoutDebtsInstruction that = (PayoutDebtsInstruction) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(toAccountId, that.toAccountId) &&
                Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, toAccountId, amount);
    }
}
