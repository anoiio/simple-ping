package com.andreykasianov.ping.task.http;

import com.andreykasianov.ping.task.TaskResultDTO;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class HttpTaskResultDTO extends TaskResultDTO {
    private int status;
    private long responseTime;
}
