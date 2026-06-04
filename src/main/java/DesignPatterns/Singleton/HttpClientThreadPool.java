package DesignPatterns.Singleton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public enum HttpClientThreadPool {
    INSTANCE;

    private final ExecutorService executor;

    HttpClientThreadPool() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}