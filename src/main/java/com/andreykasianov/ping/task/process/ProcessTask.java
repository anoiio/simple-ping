package com.andreykasianov.ping.task.process;

import com.andreykasianov.ping.task.BasePingTask;
import com.andreykasianov.ping.task.TaskResultDTO;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Abstract class for process-based ping
 */

@Slf4j
@SuperBuilder
public abstract class ProcessTask extends BasePingTask {
    protected ProcessTaskConfig config;

    @Override
    public TaskResultDTO doPing() {
        String processOutput = "";
        boolean fail = true;
        try {
            String commandStr = createProcessCommand();
            processOutput = runProcess(commandStr);
            fail = checkConditions(processOutput);

            log.debug("{} ping completed for host: {}", getTaskType(), host);
            log.trace("Process output: {}", processOutput);
        } catch (Exception e) {
            processOutput = Optional.ofNullable(e.getMessage()).orElse(e.getClass().getName());
            log.debug("Failed to execute ping task", e);
        }

        TaskResultDTO result = createTaskResult(processOutput, fail);

        return result;
    }


    protected String runProcess(String commandStr) throws IOException, InterruptedException {
        Process pr = Runtime.getRuntime().exec(commandStr);
        BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        String line;
        StringJoiner stringJoiner = new StringJoiner("\n\r");
        while ((line = in.readLine()) != null) {
            stringJoiner.add(line);
        }
        pr.waitFor();
        return stringJoiner.toString();
    }

    protected abstract TaskResultDTO createTaskResult(String processOutput, boolean fail);

    protected abstract boolean checkConditions(String processOutput);

    protected abstract String createProcessCommand();
}
