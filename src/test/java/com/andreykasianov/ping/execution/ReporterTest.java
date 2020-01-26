package com.andreykasianov.ping.execution;

import com.andreykasianov.ping.TestBase;
import com.andreykasianov.ping.report.ReportDTO;
import com.andreykasianov.ping.task.PingTaskType;
import com.andreykasianov.ping.task.TaskResultDTO;
import com.andreykasianov.ping.task.http.HttpTaskResultDTO;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReferenceArray;

@RunWith(JMockit.class)
public class ReporterTest extends TestBase {

    public static final String GOOGLE_COM = "google.com";
    public static final String CNN_COM = "cnn.com";

    public static final long G_TIMESTAMP = 1000;
    public static final String G_ICMP_OUTPUT = "GOOGLE_ICMP_OUTPUT";
    public static final String G_TRACE_OUTPUT = "GOOGLE_TRACE_OUTPUT";
    public static final String G_HTTP_EXCEPTION = "GOOGLE_HTTP_EXCEPTION";

    public static final long G_TIMESTAMP_2 = 2000;
    public static final String G_ICMP_OUTPUT_2 = "GOOGLE_ICMP_OUTPUT_2";
    public static final String G_TRACE_OUTPUT_2 = "GOOGLE_TRACE_OUTPUT_2";
    public static final String G_HTTP_EXCEPTION_2 = "GOOGLE_HTTP_EXCEPTION_2";

    public static final long CNN_TIMESTAMP = 3000;
    public static final String CNN_ICMP_OUTPUT = "CNN_ICMP_OUTPUT";
    public static final String CNN_TRACE_OUTPUT = "CNN_TRACE_OUTPUT";
    public static final String CNN_HTTP_EXCEPTION = "CNN_HTTP_EXCEPTION";

    private Reporter reporter = createReporter();
    private AtomicReferenceArray<ReportDTO> reportHolder = new AtomicReferenceArray(1);
    private AtomicReferenceArray<CountDownLatch> countDownLatchHolder = new AtomicReferenceArray(1);

    public ReporterTest() throws ConfigurationException {
    }

    @Test
    public void saveAndReportCorrectHost() throws InterruptedException {

        new MockUp<Reporter>() {
            @Mock
            private void postReport(ReportDTO report) {
                reportHolder.set(0, report);
                countDownLatchHolder.get(0).countDown();
            }
        };

        TaskResultDTO googleICMPTaskResultDTO = createTaskResult(PingTaskType.ICMP, GOOGLE_COM, G_ICMP_OUTPUT, G_TIMESTAMP);
        TaskResultDTO googleTraceTaskResultDTO = createTaskResult(PingTaskType.TRACE, GOOGLE_COM, G_TRACE_OUTPUT, G_TIMESTAMP);
        TaskResultDTO googleHttpTaskResultDTO = createHttpTaskResult(GOOGLE_COM, G_HTTP_EXCEPTION, G_TIMESTAMP);

        reporter.saveResults(googleICMPTaskResultDTO);
        reporter.saveResults(googleTraceTaskResultDTO);
        reporter.saveResults(googleHttpTaskResultDTO);

        TaskResultDTO cnnICMPTaskResultDTO = createTaskResult(PingTaskType.ICMP, CNN_COM, CNN_ICMP_OUTPUT, CNN_TIMESTAMP);
        TaskResultDTO cnnTraceTaskResultDTO = createTaskResult(PingTaskType.TRACE, CNN_COM, CNN_TRACE_OUTPUT, CNN_TIMESTAMP);
        TaskResultDTO cnnHttpTaskResultDTO = createHttpTaskResult(CNN_COM, CNN_HTTP_EXCEPTION, CNN_TIMESTAMP);

        reporter.saveResults(cnnICMPTaskResultDTO);
        reporter.saveResults(cnnTraceTaskResultDTO);
        reporter.saveResults(cnnHttpTaskResultDTO);

        countDownLatchHolder.set(0, new CountDownLatch(1));
        reporter.report(GOOGLE_COM);
        countDownLatchHolder.get(0).await();
        validateReport(reportHolder.get(0), GOOGLE_COM, G_ICMP_OUTPUT, G_TRACE_OUTPUT, G_HTTP_EXCEPTION, G_TIMESTAMP);

        countDownLatchHolder.set(0, new CountDownLatch(1));
        reporter.report(CNN_COM);
        countDownLatchHolder.get(0).await();
        validateReport(reportHolder.get(0), CNN_COM, CNN_ICMP_OUTPUT, CNN_TRACE_OUTPUT, CNN_HTTP_EXCEPTION, CNN_TIMESTAMP);
    }

    @Test
    public void saveAndReportLastResults() throws InterruptedException {

        new MockUp<Reporter>() {
            @Mock
            private void postReport(ReportDTO report) {
                reportHolder.set(0, report);
                countDownLatchHolder.get(0).countDown();
            }
        };

        TaskResultDTO googleICMPTaskResultDTO = createTaskResult(PingTaskType.ICMP, GOOGLE_COM, G_ICMP_OUTPUT, G_TIMESTAMP);
        TaskResultDTO googleTraceTaskResultDTO = createTaskResult(PingTaskType.TRACE, GOOGLE_COM, G_TRACE_OUTPUT, G_TIMESTAMP);
        TaskResultDTO googleHttpTaskResultDTO = createHttpTaskResult(GOOGLE_COM, G_HTTP_EXCEPTION, G_TIMESTAMP);

        reporter.saveResults(googleICMPTaskResultDTO);
        reporter.saveResults(googleTraceTaskResultDTO);
        reporter.saveResults(googleHttpTaskResultDTO);

        TaskResultDTO googleICMPTaskResultDTO2 = createTaskResult(PingTaskType.ICMP, GOOGLE_COM, G_ICMP_OUTPUT_2, G_TIMESTAMP_2);
        TaskResultDTO googleTraceTaskResultDTO2 = createTaskResult(PingTaskType.TRACE, GOOGLE_COM, G_TRACE_OUTPUT_2, G_TIMESTAMP_2);
        TaskResultDTO googleHttpTaskResultDTO2 = createHttpTaskResult(GOOGLE_COM, G_HTTP_EXCEPTION_2, G_TIMESTAMP_2);

        reporter.saveResults(googleICMPTaskResultDTO2);
        reporter.saveResults(googleTraceTaskResultDTO2);
        reporter.saveResults(googleHttpTaskResultDTO2);

        countDownLatchHolder.set(0, new CountDownLatch(1));
        reporter.report(GOOGLE_COM);
        countDownLatchHolder.get(0).await();
        validateReport(reportHolder.get(0), GOOGLE_COM, G_ICMP_OUTPUT_2, G_TRACE_OUTPUT_2, G_HTTP_EXCEPTION_2, G_TIMESTAMP_2);
    }

    private void validateReport(ReportDTO report,
                                String HOST,
                                String ICMP_OUTPUT,
                                String TRACE_OUTPUT,
                                String HTTP_EXCEPTION,
                                long TIMESTAMP) {

        Assert.assertEquals(HOST, report.getHost());

        Assert.assertEquals("incorrect icmp output", ICMP_OUTPUT, report.getIcmp_ping().getProcessOutput());
        Assert.assertEquals("incorrect icmp timestamp",TIMESTAMP, report.getIcmp_ping().getTimestamp(), 0);

        Assert.assertEquals("incorrect trace output",TRACE_OUTPUT, report.getTrace().getProcessOutput());
        Assert.assertEquals("incorrect trace timestamp",TIMESTAMP, report.getTrace().getTimestamp(), 0);

        Assert.assertEquals("incorrect http output",HTTP_EXCEPTION, report.getTcp_ping().getMessage());
        Assert.assertEquals("incorrect http timestamp",TIMESTAMP, report.getTcp_ping().getTimestamp(), 0);

        Assert.assertEquals("incorrect http response time",1000, report.getTcp_ping().getResponseTime());
        Assert.assertEquals("incorrect http response status",503, report.getTcp_ping().getResponseStatus());
    }

    private TaskResultDTO createTaskResult(PingTaskType taskType, String host, String processOutput, long timestamp) {
        return TaskResultDTO
                .builder()
                .result(processOutput)
                .fail(true)
                .timestamp(timestamp)
                .taskType(taskType)
                .host(host)
                .build();
    }

    private HttpTaskResultDTO createHttpTaskResult(String host, String exceptionMessage, long timestamp) {
        return HttpTaskResultDTO
                .builder()
                .taskType(PingTaskType.HTTP)
                .host(host)
                .timestamp(timestamp)
                .status(503)
                .responseTime(1000)
                .result(exceptionMessage)
                .fail(true)
                .build();
    }

    private Reporter createReporter() throws ConfigurationException {
        Reporter reporter = new Reporter(initConfiguration());
        return reporter;
    }
}