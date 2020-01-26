package com.andreykasianov.ping;

import com.andreykasianov.ping.execution.Reporter;
import com.andreykasianov.ping.execution.TaskExecutionTemplate;
import com.andreykasianov.ping.task.TaskFactory;
import com.andreykasianov.ping.task.http.HttpTaskConfig;
import com.andreykasianov.ping.task.process.ProcessTaskConfig;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestBase {
    public static final String CONFIG_PROPERTIES = "config.properties";

    protected TaskExecutionTemplate createTaskExecutionTemplate() throws ConfigurationException {
        Reporter reporter = new Reporter(initConfiguration());
        TaskExecutionTemplate taskExecutionTemplate = new TaskExecutionTemplate(reporter);
        return taskExecutionTemplate;
    }

    protected PropertiesConfiguration initConfiguration() throws org.apache.commons.configuration2.ex.ConfigurationException {
        FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                new FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class)
                        .configure(new Parameters().properties()
                                .setFileName(CONFIG_PROPERTIES)
                                .setThrowExceptionOnMissing(true)
                                .setListDelimiterHandler(new DefaultListDelimiterHandler(';'))
                                .setIncludesAllowed(false));
        return builder.getConfiguration();
    }

    protected TaskFactory createTaskFactory(PropertiesConfiguration properties) {
        String icmpCommand = properties.getString("icmp.command", "ping -n 5");
        String traceCommand = properties.getString("trace.command", "tracert ");
        int connectTimeout = properties.getInt("http.connect.timeout.milliseconds", 10_000);
        int readTimeout = properties.getInt("http.read.timeout.milliseconds", 20_000);
        int responseTimeThreshold = properties.getInt("http.response.time.threshold.milliseconds", 15_000);
        List<Integer> invalidCodes = properties.getList(Integer.class, "http.invalid.codes", Arrays.asList(502, 503, 504, 599));

        ProcessTaskConfig icmpConfig = new ProcessTaskConfig(icmpCommand);
        ProcessTaskConfig traceConfig = new ProcessTaskConfig(traceCommand);
        HttpTaskConfig httpTaskConfig = new HttpTaskConfig(connectTimeout, readTimeout, responseTimeThreshold, new HashSet<Integer>(invalidCodes));

        return new TaskFactory(icmpConfig, traceConfig, httpTaskConfig);
    }

    protected void sleep(long timeout) {
        try {
            TimeUnit.MILLISECONDS.sleep(timeout);
        } catch (Exception e) {
            // ignore
        }
    }
}
