package com.andreykasianov.ping.task;

import com.andreykasianov.ping.task.http.HttpPingTask;
import com.andreykasianov.ping.task.http.HttpTaskConfig;
import com.andreykasianov.ping.task.process.ProcessTaskConfig;
import com.andreykasianov.ping.task.process.icmp.ICMPPingTask;
import com.andreykasianov.ping.task.process.trace.TraceTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * TaskFactory creates and populates ping tasks for the given host and type
 */

@Slf4j
@AllArgsConstructor
public class TaskFactory {

    private ProcessTaskConfig icmpTaskConfig;
    private ProcessTaskConfig traceTaskConfig;
    private HttpTaskConfig httpTaskConfig;

    public Optional<PingTask> getTask(PingTaskType taskType, String host) {
        switch (taskType) {
            case ICMP:
                return Optional.of(ICMPPingTask
                        .builder()
                        .config(icmpTaskConfig)
                        .host(host)
                        .build());
            case TRACE:
                return Optional.of(TraceTask
                        .builder()
                        .config(traceTaskConfig)
                        .host(host)
                        .build());
            case HTTP:
                return Optional.of(HttpPingTask
                        .builder()
                        .config(httpTaskConfig)
                        .host(host)
                        .build());
            default:
                log.error("Not supported PingTaskType:{}", taskType);
                return Optional.empty();
        }
    }
}