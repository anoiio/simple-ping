package com.andreykasianov.ping.execution;

import com.andreykasianov.ping.TestBase;
import com.andreykasianov.ping.task.PingTaskType;
import com.andreykasianov.ping.task.TaskResultDTO;
import com.andreykasianov.ping.task.process.ProcessTaskConfig;
import com.andreykasianov.ping.task.process.icmp.ICMPPingTask;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

@RunWith(JMockit.class)
public class TaskExecutionTemplateTest extends TestBase {

    public static final String GOOGLE_COM = "google.com";
    private TaskExecutionTemplate taskExecutionTemplate = createTaskExecutionTemplate();

    private ICMPPingTask icmpPingTask;
    private CountDownLatch ping;
    private CountDownLatch saveResults;
    private CountDownLatch report;

    public TaskExecutionTemplateTest() throws ConfigurationException {
    }

    @Before
    public void initTest() {
        ping = new CountDownLatch(1);
        saveResults = new CountDownLatch(1);
        report = new CountDownLatch(1);
        icmpPingTask = createICMPPingTask();

        new MockUp<Reporter>() {
            @Mock
            public void saveResults(TaskResultDTO taskResult) {
                saveResults.countDown();
            }

            @Mock
            public void report(String host) {
                report.countDown();
            }
        };
    }

    @Test
    public void runSuccess() {
        new MockUp<ICMPPingTask>() {
            @Mock
            public final TaskResultDTO ping() {
                ping.countDown();
                return TaskResultDTO.builder()
                        .result("processOutput")
                        .fail(false)
                        .timestamp(System.currentTimeMillis())
                        .taskType(PingTaskType.ICMP)
                        .host(GOOGLE_COM)
                        .build();
            }
        };

        taskExecutionTemplate.run(icmpPingTask);

        Assert.assertEquals("ping task was not called", 0, ping.getCount());
        Assert.assertEquals("save results was not called", 0, saveResults.getCount());
        Assert.assertEquals("report should not be called", 1, report.getCount());
    }

    @Test
    public void runFail() {
        new MockUp<ICMPPingTask>() {
            @Mock
            public final TaskResultDTO ping() {
                ping.countDown();
                return TaskResultDTO.builder()
                        .result("processOutput")
                        .fail(true)
                        .timestamp(System.currentTimeMillis())
                        .taskType(PingTaskType.ICMP)
                        .host(GOOGLE_COM)
                        .build();
            }
        };

        taskExecutionTemplate.run(icmpPingTask);

        Assert.assertEquals("ping task was not called", 0, ping.getCount());
        Assert.assertEquals("save results was not called", 0, saveResults.getCount());
        Assert.assertEquals("report was not called", 0, report.getCount());
    }

    @Test
    public void runSkip() {
        new MockUp<ICMPPingTask>() {
            @Mock
            public final TaskResultDTO ping() {
                ping.countDown();
                return TaskResultDTO.builder()
                        .result("processOutput")
                        .fail(false)
                        .timestamp(System.currentTimeMillis())
                        .taskType(PingTaskType.ICMP)
                        .host(GOOGLE_COM)
                        .build();
            }

            @Mock
            public boolean canRun() {
                return false;
            }
        };


        taskExecutionTemplate.run(icmpPingTask);

        Assert.assertEquals("ping task should not be called", 1, ping.getCount());
        Assert.assertEquals("save results should not be called", 1, saveResults.getCount());
        Assert.assertEquals("report should not be called", 1, report.getCount());
    }

    private ICMPPingTask createICMPPingTask() {
        ProcessTaskConfig processTaskConfig = new ProcessTaskConfig("ping -n 5");
        return ICMPPingTask.builder()
                .host(GOOGLE_COM)
                .config(processTaskConfig)
                .build();
    }

}