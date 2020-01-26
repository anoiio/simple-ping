package com.andreykasianov.ping.task.process.icmp;

import com.andreykasianov.ping.task.PingTaskType;
import com.andreykasianov.ping.task.TaskResultDTO;
import com.andreykasianov.ping.task.process.ProcessTask;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ICMP ping implementation
 */

@SuperBuilder
@Slf4j
public class ICMPPingTask extends ProcessTask {

    private static final Pattern patternLost = Pattern.compile("Lost = 0");
    private static final Pattern patternTimeout = Pattern.compile("Request timed out");

    @Override
    protected TaskResultDTO createTaskResult(String processOutput, boolean fail) {
        return TaskResultDTO
                .builder()
                .result(processOutput)
                .fail(fail)
                .timestamp(System.currentTimeMillis())
                .taskType(getTaskType())
                .host(host)
                .build();
    }

    @Override
    protected String createProcessCommand() {
        return config.getCommand() + " " + host;
    }

    @Override
    protected boolean checkConditions(String processOutput) {

        Matcher timeoutMatcher = patternTimeout.matcher(processOutput);
        if (timeoutMatcher.find()) {
            return true;
        }

        Matcher lostMatcher = patternLost.matcher(processOutput);
        if (!lostMatcher.find()) {
            return true;
        }

        return false;
    }

    public PingTaskType getTaskType() {
        return PingTaskType.ICMP;
    }
}
