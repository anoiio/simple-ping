package com.andreykasianov.ping.report;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class PingReportDTO {
    private Long timestamp;
}
