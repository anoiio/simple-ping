package com.andreykasianov.ping.execution;

import com.andreykasianov.ping.TestBase;
import com.andreykasianov.ping.task.TaskResultDTO;
import com.andreykasianov.ping.task.http.HttpPingTask;
import com.andreykasianov.ping.task.http.HttpTaskResultDTO;
import com.andreykasianov.ping.task.process.icmp.ICMPPingTask;
import com.andreykasianov.ping.task.process.trace.TraceTask;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(JMockit.class)
public class ExecutionManagerTest extends TestBase {

    public static final List<String> HOSTS = Arrays.asList("google.com");
    public static final List<String> HOSTS_50 = Arrays.asList("aaaa.com", "baaa.com", "caaa.com", "daaa.com", "eaaa.com", "faaa.com", "gaaa.com", "haaa.com", "iaaa.com", "jaaa.com", "kaaa.com", "laaa.com", "maaa.com", "naaa.com", "oaaa.com", "paaa.com", "qaaa.com", "raaa.com", "saaa.com", "taaa.com", "uaaa.com", "vaaa.com", "waaa.com", "xaaa.com", "yaaa.com", "zaaa.com", "abaa.com", "bbaa.com", "cbaa.com", "dbaa.com", "ebaa.com", "fbaa.com", "gbaa.com", "hbaa.com", "ibaa.com", "jbaa.com", "kbaa.com", "lbaa.com", "mbaa.com", "nbaa.com", "obaa.com", "pbaa.com", "qbaa.com", "rbaa.com", "sbaa.com", "tbaa.com", "ubaa.com", "vbaa.com", "wbaa.com", "xbaa.com");


    @Test
    public void executeBasic() throws ConfigurationException {
        execute(HOSTS);
    }

    @Test
    public void execute50Hosts() throws ConfigurationException {
        execute(HOSTS_50);
    }


    private void execute(List<String> hosts) throws ConfigurationException {
        ExecutionManager executionManager = createExecutionManager(hosts);

        AtomicInteger httpPingCount = new AtomicInteger(0);
        AtomicInteger icmpPingCount = new AtomicInteger(0);
        AtomicInteger tracePingCount = new AtomicInteger(0);

        new MockUp<Reporter>() {
            @Mock
            public void report(String host) {
            }

            @Mock
            public void saveResults(TaskResultDTO taskResult) {

            }
        };

        new MockUp<HttpPingTask>() {
            @Mock
            public final TaskResultDTO doPing() {
                sleep(200);
                httpPingCount.getAndIncrement();
                return HttpTaskResultDTO.builder().host("somehost.com").build();
            }
        };
        new MockUp<ICMPPingTask>() {
            @Mock
            public final TaskResultDTO doPing() {
                sleep(1100);
                icmpPingCount.getAndIncrement();
                return TaskResultDTO.builder().host("somehost.com").build();
            }
        };

        new MockUp<TraceTask>() {
            @Mock
            public final TaskResultDTO doPing() {
                sleep(2500);
                tracePingCount.getAndIncrement();
                return TaskResultDTO.builder().host("somehost.com").build();
            }
        };

        executionManager.start();
        sleep(16_550);
        executionManager.stop();

        Assert.assertEquals("Incorrect number of the http pings", 17 * hosts.size(), httpPingCount.get());
        Assert.assertEquals("Incorrect number of the icmp pings", 11 * hosts.size(), icmpPingCount.get());
        Assert.assertEquals("Incorrect number of the trace pings", 4 * hosts.size(), tracePingCount.get());
    }


    private ExecutionManager createExecutionManager(List<String> hosts) throws ConfigurationException {
        PropertiesConfiguration configuration = initConfiguration();
        ExecutionManager executionManager = new ExecutionManager(
                createTaskExecutionTemplate(),
                createTaskFactory(configuration),
                configuration,
                hosts);

        return executionManager;
    }
}