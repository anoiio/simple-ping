package com.andreykasianov.ping.report;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class ProcessPingReportDTO extends PingReportDTO {
    private String processOutput;
}
