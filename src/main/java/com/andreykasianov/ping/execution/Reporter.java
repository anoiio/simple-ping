package com.andreykasianov.ping.execution;

import com.andreykasianov.ping.report.HttpPingReportDTO;
import com.andreykasianov.ping.report.ProcessPingReportDTO;
import com.andreykasianov.ping.report.ReportDTO;
import com.andreykasianov.ping.task.PingTaskType;
import com.andreykasianov.ping.task.TaskResultDTO;
import com.andreykasianov.ping.task.http.HttpTaskResultDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/***
 * Reporter save last results of the ping tasks and reports in the case of ping fail
 */
@Slf4j
public class Reporter {

    static final Logger reportLogger = LogManager.getLogger("report.logger");

    private String url;
    protected Client client;
    private Map<String, Map<PingTaskType, TaskResultDTO>> lastResults = new ConcurrentHashMap<>();
    ExecutorService executor = Executors.newFixedThreadPool(5);
    ObjectMapper objectMapper = new ObjectMapper();

    public Reporter(PropertiesConfiguration properties) {
        this.url = properties.getString("report.url");
        ClientConfig config = new ClientConfig();
        config.property(ClientProperties.CONNECT_TIMEOUT, 10_000)
                .property(ClientProperties.READ_TIMEOUT, 60_000);
        client = ClientBuilder.newClient(config);
    }

    /**
     * Saves last ping result
     * @param taskResult - ping task result
     */
    public void saveResults(TaskResultDTO taskResult) {
        log.debug("saving {} results for host: {}", taskResult.getTaskType(), taskResult.getHost());
        log.trace("taskResult: {}", taskResult);
        Map<PingTaskType, TaskResultDTO> hostPingResults = lastResults.computeIfAbsent(taskResult.getHost(), k -> new HashMap<>());
        hostPingResults.put(taskResult.getTaskType(), taskResult);
    }

    /**
     * Creates report for the given host and asynchronously do POST call
     * @param host
     */
    public void report(String host) {
        log.debug("start report for host: {}", host);
        Map<PingTaskType, TaskResultDTO> hostPingResults = lastResults.get(host);

        ReportDTO report = ReportDTO.builder()
                .host(host)
                .trace(createProcessPingReport(hostPingResults.get(PingTaskType.TRACE)))
                .icmp_ping(createProcessPingReport(hostPingResults.get(PingTaskType.ICMP)))
                .tcp_ping(createHttpPingReport(hostPingResults.get(PingTaskType.HTTP)))
                .build();

        executor.execute(() -> postReport(report));
    }

    /**
     * Creates ICMP/TRACE report parts
     * @param taskResult - ICMP/TRACE result
     * @return
     */
    private ProcessPingReportDTO createProcessPingReport(TaskResultDTO taskResult) {

        if (taskResult == null) {
            return null;
        }

        ProcessPingReportDTO pingReportDTO = ProcessPingReportDTO.builder()
                .processOutput(taskResult.getResult())
                .timestamp(taskResult.getTimestamp())
                .build();

        return pingReportDTO;
    }

    /**
     * Creates tcp report part
     * @param taskResult
     * @return
     */
    private HttpPingReportDTO createHttpPingReport(TaskResultDTO taskResult) {

        if (!(taskResult instanceof HttpTaskResultDTO)) {
            return null;
        }
        HttpTaskResultDTO httpTaskResult = (HttpTaskResultDTO) taskResult;
        HttpPingReportDTO pingReportDTO = HttpPingReportDTO.builder()
                .timestamp(httpTaskResult.getTimestamp())
                .responseStatus(httpTaskResult.getStatus())
                .message(httpTaskResult.getResult())
                .responseTime(httpTaskResult.getResponseTime())
                .build();

        return pingReportDTO;
    }

    /**
     * Performs POST call with report
     * @param report
     */
    private void postReport(ReportDTO report) {
        String jsonReport = toJSON(report);
        log.debug("posting report: {} to url: {}", jsonReport, url);
        reportLogger.warn(jsonReport);
        try {
            WebTarget webTarget = client.target(url);
            Response response = webTarget
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jsonReport, MediaType.APPLICATION_JSON_TYPE));

            int status = response.getStatus();
            if (status != 200) {
                log.error("Report failed with status {}", status);
            }
        } catch (Exception e) {
            log.error("Report failed");
        }
    }

    /**
     * Creates report JSON string
     * @param report
     * @return
     */
    private String toJSON(ReportDTO report) {
        try {
            String jsonReport = objectMapper.writeValueAsString(report);
            return jsonReport;
        } catch (JsonProcessingException e) {
            log.error("Failed to log report", e);
            return null;
        }
    }
}
