package com.andreykasianov.ping.task.http;

import com.andreykasianov.ping.task.PingTaskType;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

@RunWith(JMockit.class)
public class HttpPingTaskTest {

    public static final String GOOGLE_COM = "google.com";
    private HttpPingTask httpPingTask = createHttpPingTask();

    @Test
    public void doPingSuccess() {

        new MockUp<HttpPingTask>() {
            @Mock
            private Response performHttpCall() {
                ResponseMock response = ResponseMock.builder().status(200).build();
                return response;
            }
        };

        HttpTaskResultDTO result = (HttpTaskResultDTO) httpPingTask.ping();

        Assert.assertFalse("Ping should finish with success", result.isFail());
        Assert.assertEquals("Incorrect host", GOOGLE_COM, result.getHost());
        Assert.assertEquals("Incorrect task type", PingTaskType.HTTP, result.getTaskType());
        Assert.assertTrue("Missing timestamp", result.getTimestamp() > 0);
        Assert.assertTrue("Invalid response time", result.getResponseTime() >= 0);
        Assert.assertEquals("Invalid response code", 200, result.getStatus());
        Assert.assertNull("Result string should be empty on success", result.getResult());
    }

    @Test
    public void doPingInvalidCode() {

        new MockUp<HttpPingTask>() {
            @Mock
            private Response performHttpCall() {
                ResponseMock response = ResponseMock.builder().status(503).build();
                return response;
            }
        };

        HttpTaskResultDTO result = (HttpTaskResultDTO) httpPingTask.ping();

        Assert.assertTrue("Ping should finish with fail", result.isFail());
        Assert.assertEquals("Incorrect host", GOOGLE_COM, result.getHost());
        Assert.assertEquals("Incorrect task type", PingTaskType.HTTP, result.getTaskType());
        Assert.assertTrue("Missing timestamp", result.getTimestamp() > 0);
        Assert.assertTrue("Invalid response time", result.getResponseTime() >= 0);
        Assert.assertEquals("Invalid response code", 503, result.getStatus());
        Assert.assertNull("Result string should be empty on invalid code", result.getResult());
    }

    @Test
    public void doPingException() {

        new MockUp<HttpPingTask>() {
            @Mock
            private Response performHttpCall() {
                throw new ProcessingException(new java.net.UnknownHostException());
            }
        };

        HttpTaskResultDTO result = (HttpTaskResultDTO) httpPingTask.ping();

        Assert.assertTrue("Ping should finish with fail", result.isFail());
        Assert.assertEquals("Incorrect host", GOOGLE_COM, result.getHost());
        Assert.assertEquals("Incorrect task type", PingTaskType.HTTP, result.getTaskType());
        Assert.assertTrue("Missing timestamp", result.getTimestamp() > 0);
        Assert.assertTrue("Invalid response time", result.getResponseTime() >= 0);
        Assert.assertEquals("Should not be response code in case of exception", -1, result.getStatus());
        Assert.assertEquals("Result string should not be empty", "java.net.UnknownHostException", result.getResult());
    }

    @Test
    public void doPingThreshold() {

        new MockUp<HttpPingTask>() {
            @Mock
            private Response performHttpCall() {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
                ResponseMock response = ResponseMock.builder().status(200).build();
                return response;
            }
        };

        HttpTaskResultDTO result = (HttpTaskResultDTO) httpPingTask.ping();

        Assert.assertTrue("Ping should finish with fail", result.isFail());
        Assert.assertEquals("Incorrect host", GOOGLE_COM, result.getHost());
        Assert.assertEquals("Incorrect task type", PingTaskType.HTTP, result.getTaskType());
        Assert.assertTrue("Missing timestamp", result.getTimestamp() > 0);
        Assert.assertTrue("Invalid response time", result.getResponseTime() >= 1000 && result.getResponseTime() < 1100);
        Assert.assertEquals("Invalid response code", 200, result.getStatus());
        Assert.assertNull("Result string should be empty on threshold fail", result.getResult());
    }

    private HttpPingTask createHttpPingTask() {
        HashSet<Integer> invalidCodes = new HashSet<>();
        invalidCodes.add(Integer.valueOf(503));
        HttpTaskConfig httpTaskConfig = new HttpTaskConfig(1000, 1000, 500, invalidCodes);
        return HttpPingTask.builder()
                .host(GOOGLE_COM)
                .config(httpTaskConfig)
                .build();
    }
}