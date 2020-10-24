package com.ocbc.bank.domain;

import com.ocbc.bank.eventsource.UserAccountEventSource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserAccountRepository {
    private Map<String, UserAccount> userAccountRepo;
    private UserAccountEventSource userAccountEventSource;

    public UserAccountRepository(UserAccountEventSource userAccountEventSource) {
        this.userAccountRepo = new ConcurrentHashMap<>();
        this.userAccountEventSource = userAccountEventSource;
    }

    /**
     * Blocking call to get access of UserAccount bean
     * If not present then it will be created by replaying transaction log for the user account id
     * call will be blocked while loading is in progress
     *
     * @param userAccountId
     * @return
     */
    public UserAccount getUserAccount(String userAccountId) {
        return this.userAccountRepo.compute(userAccountId, (k,v) -> {
            if(v == null) {
                v = new UserAccount(userAccountId, userAccountEventSource);
            }
            return v;
        } );
    }


    public UserAccount getIfPresent(String userAccountId) {
        return this.userAccountRepo.get(userAccountId);
    }
}
