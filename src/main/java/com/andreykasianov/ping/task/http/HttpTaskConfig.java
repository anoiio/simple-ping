package com.andreykasianov.ping.task.http;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashSet;

@Data
@AllArgsConstructor
public class HttpTaskConfig {
    private int connectTimeout;
    private int readTimeout;
    private int responseTimeThreshold;
    private HashSet<Integer> invalidCodes;
}
