package com.andreykasianov.ping.task.http;

import com.andreykasianov.ping.task.BasePingTask;
import com.andreykasianov.ping.task.PingTaskType;
import com.andreykasianov.ping.task.TaskResultDTO;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * HTTP ping implementation
 */

@Slf4j
@SuperBuilder
public class HttpPingTask extends BasePingTask {
    private HttpTaskConfig config;
    private Client client;


    private void initClient() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, config.getConnectTimeout())
                .property(ClientProperties.READ_TIMEOUT, config.getReadTimeout());
        client = ClientBuilder.newClient(clientConfig);
    }

    @Override
    public TaskResultDTO doPing() {
        if (client == null) {
            initClient();
        }
        String exceptionMessage = null;
        boolean fail = true;
        int status = -1;
        long responseTime;
        long start = System.currentTimeMillis();
        try {
            Response response = performHttpCall();
            responseTime = System.currentTimeMillis() - start;
            status = response.getStatus();
            fail = checkConditions(status, responseTime);
        } catch (Exception e) {
            exceptionMessage = Optional.ofNullable(e.getMessage()).orElse(e.getClass().getName());
            log.debug("ping failed: {}", exceptionMessage);
            responseTime = System.currentTimeMillis() - start;
        }

        log.debug("{} ping completed for {} with status {} msg: '{}' within {} ms", getTaskType(), host, status, exceptionMessage, responseTime);
        return HttpTaskResultDTO
                .builder()
                .taskType(getTaskType())
                .host(host)
                .timestamp(System.currentTimeMillis())
                .status(status)
                .responseTime(responseTime)
                .result(exceptionMessage)
                .fail(fail)
                .build();
    }

    private Response performHttpCall() {
        WebTarget webTarget = client.target("https://" + host);
        return webTarget
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
    }

    private boolean checkConditions(int status, long duration) {
        if (config.getInvalidCodes().contains(status)) {
            log.debug("ping failed with status {}", status);
        } else if (duration >= config.getResponseTimeThreshold()) {
            log.debug("ping duration exceeded. threshold: {} duration: {}", config.getResponseTimeThreshold(), duration);
        } else if (duration < config.getResponseTimeThreshold()) {
            return false;
        }
        return true;
    }

    public PingTaskType getTaskType() {
        return PingTaskType.HTTP;
    }
}
