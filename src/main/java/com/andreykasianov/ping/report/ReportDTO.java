package com.andreykasianov.ping.report;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportDTO {
    private String host;
    private ProcessPingReportDTO icmp_ping;
    private HttpPingReportDTO tcp_ping;
    private ProcessPingReportDTO trace;
}
