package com.andreykasianov.ping.task.process.trace;

import com.andreykasianov.ping.task.PingTaskType;
import com.andreykasianov.ping.task.TaskResultDTO;
import com.andreykasianov.ping.task.process.ProcessTask;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Trace ping implementation
 */

@Slf4j
@SuperBuilder
public class TraceTask extends ProcessTask {

    @Override
    protected TaskResultDTO createTaskResult(String processOutput, boolean fail) {
        return TaskResultDTO
                .builder()
                .taskType(getTaskType())
                .host(host)
                .timestamp(System.currentTimeMillis())
                .result(processOutput)
                .fail(false)
                .build();
    }

    @Override
    protected boolean checkConditions(String processOutput) {
        return false;
    }

    @Override
    protected String createProcessCommand() {
        return config.getCommand() + " " + host;
    }

    public PingTaskType getTaskType() {
        return PingTaskType.TRACE;
    }
}
