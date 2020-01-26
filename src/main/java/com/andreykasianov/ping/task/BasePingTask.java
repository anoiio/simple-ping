package com.andreykasianov.ping.task;


import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.concurrent.atomic.AtomicBoolean;

@SuperBuilder
@Getter
public abstract class BasePingTask implements PingTask {
    private final AtomicBoolean running = new AtomicBoolean(false);
    protected String host;

    @Override
    public final TaskResultDTO ping() {
        running.set(true);
        try {
            return doPing();
        } finally {
            running.set(false);
        }
    }

    public abstract TaskResultDTO doPing();

    public boolean canRun() {
        return !running.get();
    }
}
