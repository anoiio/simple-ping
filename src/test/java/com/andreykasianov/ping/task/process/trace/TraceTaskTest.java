package com.andreykasianov.ping.task.process.trace;

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
public class TraceTaskTest {

    public static final String OUTPUT =
            "Tracing route to google.com [172.217.171.206]\n" +
                    "over a maximum of 30 hops:\n" +
                    "\n" +
                    "  1     1 ms    <1 ms     1 ms  192.168.0.1\n" +
                    "  2     7 ms     7 ms     9 ms  10.180.60.1\n" +
                    "  3    10 ms    10 ms    12 ms  172.18.6.218\n" +
                    "  4     9 ms     *        *     172.17.3.66\n" +
                    "  5     8 ms     8 ms    14 ms  BB302-PT-e49-100G-HOT.israelinternet.co.il [185.149.252.109]\n" +
                    "  6    14 ms    22 ms    15 ms  dynamic-141-226-122-130.israelinternet.co.il [141.226.122.130]\n" +
                    "  7    13 ms    14 ms    13 ms  bzq-84-110-49-250.cablep.bezeqint.net [84.110.49.250]\n" +
                    "  8    11 ms     9 ms    10 ms  10.90.99.13\n" +
                    "  9    47 ms    50 ms    49 ms  74.125.51.88\n" +
                    " 10    49 ms    48 ms    49 ms  74.125.244.225\n" +
                    " 11    53 ms    51 ms    51 ms  216.239.50.123\n" +
                    " 12    49 ms     *       53 ms  mrs09s06-in-f14.1e100.net [172.217.171.206]\n" +
                    "\n" +
                    "Trace complete.";


    public static final String GOOGLE_COM = "google.com";
    private TraceTask traceTask = createTraceTask();

    @Test
    public void doPing() {
        new MockUp<TraceTask>() {
            @Mock
            protected String runProcess(String commandStr) throws IOException, InterruptedException {
                return OUTPUT;
            }
        };

        TaskResultDTO result = traceTask.ping();
        Assert.assertFalse("Ping should finish with success", result.isFail());
        Assert.assertEquals("Incorrect host", GOOGLE_COM, result.getHost());
        Assert.assertEquals("Incorrect task type", PingTaskType.TRACE, result.getTaskType());
        Assert.assertEquals("Incorrect task output", OUTPUT, result.getResult());
        Assert.assertTrue("Missing timestamp", result.getTimestamp() > 0);
    }


    private TraceTask createTraceTask() {
        ;
        ProcessTaskConfig processTaskConfig = new ProcessTaskConfig("tracert");
        return TraceTask.builder()
                .host(GOOGLE_COM)
                .config(processTaskConfig)
                .build();
    }
}