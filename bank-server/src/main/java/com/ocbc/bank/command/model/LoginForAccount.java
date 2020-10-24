package com.ocbc.bank.command.model;

public class LoginForAccount implements IBankingInstruction {
    private final String accountId;

    public LoginForAccount(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }

    @Override
    public String toString() {
        return "LoginForAccount{" +
                "accountId='" + accountId + '\'' +
                '}';
    }
}
