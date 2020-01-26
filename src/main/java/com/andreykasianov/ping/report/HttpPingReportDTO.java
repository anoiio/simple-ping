package com.andreykasianov.ping.report;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class HttpPingReportDTO extends PingReportDTO {
    private int responseStatus;
    private long responseTime;
    private String message;
}

