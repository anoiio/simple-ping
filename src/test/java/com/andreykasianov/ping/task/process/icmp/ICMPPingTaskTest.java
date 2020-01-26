package com.andreykasianov.ping.task.process.icmp;

import com.andreykasianov.ping.task.PingTaskType;
import com.andreykasianov.ping.task.TaskResultDTO;
import com.andreykasianov.ping.task.process.ProcessTaskConfig;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(JMockit.class)
public class ICMPPingTaskTest {


    public static final String SUCCESS_OUTPUT =
            "Pinging google.com [109.71.161.166] with 32 bytes of data:\n" +
                    "Reply from 109.71.161.166: bytes=32 time=90ms TTL=52\n" +
                    "Reply from 109.71.161.166: bytes=32 time=85ms TTL=52\n" +
                    "Reply from 109.71.161.166: bytes=32 time=85ms TTL=52\n" +
                    "Reply from 109.71.161.166: bytes=32 time=84ms TTL=52\n" +
                    "Reply from 109.71.161.166: bytes=32 time=86ms TTL=52\n" +
                    "\n" +
                    "Ping statistics for 109.71.161.166:\n" +
                    "    Packets: Sent = 5, Received = 5, Lost = 0 (0% loss),\n" +
                    "Approximate round trip times in milli-seconds:\n" +
                    "    Minimum = 84ms, Maximum = 90ms, Average = 86ms";

    public static final String TIMEOUT_OUTPUT =
            "Pinging google.com [52.50.65.32] with 32 bytes of data:\n" +
                    "Request timed out.\n" +
                    "Request timed out.\n" +
                    "Request timed out.\n" +
                    "Request timed out.\n" +
                    "Request timed out.\n" +
                    "\n" +
                    "Ping statistics for 52.50.65.32:\n" +
                    "    Packets: Sent = 5, Received = 0, Lost = 5 (100% loss),";

    public static final String LOST_OUTPUT =
            "Pinging google.com [109.71.161.166] with 32 bytes of data:\n" +
                    "Reply from 109.71.161.166: bytes=32 time=90ms TTL=52\n" +
                    "Reply from 109.71.161.166: bytes=32 time=85ms TTL=52\n" +
                    "Reply from 109.71.161.166: bytes=32 time=85ms TTL=52\n" +
                    "Unknown error.\n" +
                    "Reply from 109.71.161.166: bytes=32 time=86ms TTL=52\n" +
                    "\n" +
                    "Ping statistics for 109.71.161.166:\n" +
                    "    Packets: Sent = 5, Received = 4, Lost = 1 (20% loss),\n" +
                    "Approximate round trip times in milli-seconds:\n" +
                    "    Minimum = 84ms, Maximum = 90ms, Average = 86ms";


    public static final String GOOGLE_COM = "google.com";
    private ICMPPingTask icmpPingTask = createICMPPingTask();

    @Test
    public void doPingSuccess() {
        new MockUp<ICMPPingTask>() {
            @Mock
            protected String runProcess(String commandStr) throws IOException, InterruptedException {
                return SUCCESS_OUTPUT;
            }
        };

        TaskResultDTO result = icmpPingTask.ping();
        Assert.assertFalse("Ping should finish with success", result.isFail());
        Assert.assertEquals("Incorrect host", GOOGLE_COM, result.getHost());
        Assert.assertEquals("Incorrect task type", PingTaskType.ICMP, result.getTaskType());
        Assert.assertEquals("Incorrect task output", SUCCESS_OUTPUT, result.getResult());
        Assert.assertTrue("Missing timestamp", result.getTimestamp() > 0);
    }

    @Test
    public void doPingTimeOut() {
        new MockUp<ICMPPingTask>() {
            @Mock
            protected String runProcess(String commandStr) throws IOException, InterruptedException {
                return TIMEOUT_OUTPUT;
            }
        };

        TaskResultDTO result = icmpPingTask.ping();
        Assert.assertTrue("Ping should finish with success", result.isFail());
        Assert.assertEquals("Incorrect host", GOOGLE_COM, result.getHost());
        Assert.assertEquals("Incorrect task type", PingTaskType.ICMP, result.getTaskType());
        Assert.assertEquals("Incorrect task output", TIMEOUT_OUTPUT, result.getResult());
        Assert.assertTrue("Missing timestamp", result.getTimestamp() > 0);
    }

    @Test
    public void doPingLost() {
        new MockUp<ICMPPingTask>() {
            @Mock
            protected String runProcess(String commandStr) throws IOException, InterruptedException {
                return LOST_OUTPUT;
            }
        };

        TaskResultDTO result = icmpPingTask.ping();
        Assert.assertTrue("Ping should finish with success", result.isFail());
        Assert.assertEquals("Incorrect host", GOOGLE_COM, result.getHost());
        Assert.assertEquals("Incorrect task type", PingTaskType.ICMP, result.getTaskType());
        Assert.assertEquals("Incorrect task output", LOST_OUTPUT, result.getResult());
        Assert.assertTrue("Missing timestamp", result.getTimestamp() > 0);
    }

    private ICMPPingTask createICMPPingTask() {
        ProcessTaskConfig processTaskConfig = new ProcessTaskConfig("ping -n 5");
        return ICMPPingTask.builder()
                .host(GOOGLE_COM)
                .config(processTaskConfig)
                .build();
    }

}