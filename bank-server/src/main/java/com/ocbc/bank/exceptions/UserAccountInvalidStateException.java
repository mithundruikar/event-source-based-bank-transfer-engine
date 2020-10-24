package com.ocbc.bank.exceptions;

public class UserAccountInvalidStateException extends Exception {
    public UserAccountInvalidStateException(String accountId, String msg) {
        super(accountId + " " + msg);
    }
}
