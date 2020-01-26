package com.andreykasianov.ping.task;

public interface PingTask {
    TaskResultDTO ping();
    boolean canRun();
    PingTaskType getTaskType();
    String getHost();
}
