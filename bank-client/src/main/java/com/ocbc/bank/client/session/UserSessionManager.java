package com.ocbc.bank.client.session;

public class UserSessionManager {
    private UserSession userSession;

    public void login(String userAccountId) {
        userSession = new UserSession(userAccountId);
    }

    public UserSession getUserSession() {
        return userSession;
    }
}
