package com.andreykasianov.ping.execution;

import com.andreykasianov.ping.task.PingTask;
import com.andreykasianov.ping.task.TaskResultDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Template of the ping task flow
 */

@Slf4j
@AllArgsConstructor
public class TaskExecutionTemplate {
    private Reporter reporter;

    /**
     * Runs task throw all the steps
     * @param task - task to run
     */
    public void run(PingTask task) {

        if (!canRun(task)) {
            log.debug("Skipping {} ping execution for {}", task.getTaskType(), task.getHost());
            return;
        }

        TaskResultDTO taskResult = execute(task);
        saveResults(taskResult);

        if (taskResult.isFail()) {
            report(taskResult.getHost());
        }
    }

    /**
     * Check if given task can run.
     * Task run have to be skipped if previously scheduled task of the save type for the given host still running.
     * @param task
     * @return
     */
    private boolean canRun(PingTask task) {
        return task.canRun();
    }

    /**
     * Ping given task
     * @param task
     * @return
     */
    private TaskResultDTO execute(PingTask task) {
        return task.ping();
    }

    /**
     * Save results of the ping task
     * @param taskResult
     */
    private void saveResults(TaskResultDTO taskResult) {
        reporter.saveResults(taskResult);
    }

    /**
     * Trigger report for the given host
     * @param host
     */
    private void report(String host) {
        reporter.report(host);
    }
}
