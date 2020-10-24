package com.ocbc.bank.command.dispatcher;

import java.util.concurrent.CompletableFuture;

public class AsyncEventDispatcher {

    public boolean dispatch(Runnable runnable) {
        CompletableFuture
                .runAsync(runnable)
                .thenAccept((events) -> {
                    // log events to traceble log
                }).exceptionally(throwable -> {
            // log error
            return null;
        });
        return true;
    }
}
