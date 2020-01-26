package com.andreykasianov.ping.task;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class TaskResultDTO {
    private boolean fail;
    private String result;
    private long timestamp;
    private PingTaskType taskType;
    private String host;
}
