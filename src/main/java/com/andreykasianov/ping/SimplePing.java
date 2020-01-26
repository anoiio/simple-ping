package com.andreykasianov.ping;

import com.andreykasianov.ping.execution.ExecutionManager;
import com.andreykasianov.ping.execution.Reporter;
import com.andreykasianov.ping.execution.TaskExecutionTemplate;
import com.andreykasianov.ping.task.TaskFactory;
import com.andreykasianov.ping.task.http.HttpTaskConfig;
import com.andreykasianov.ping.task.process.ProcessTaskConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SimplePing {

    public static final String DEFAULT_CONFIG_PROPERTIES = "config.properties";
    public static final String CONFIG_PROPERTIES = "./conf/config.properties";
    public static final String DEFAULT_HOSTS_CSV = "hosts.csv";
    public static final String HOSTS_CSV = "./conf/hosts.csv";
    public static final String CONF_LOG4J = "./conf/log4j2.xml";

    private ExecutionManager executionManager;

    public static void main(String[] args) {
        SimplePing sp = new SimplePing();
        sp.init();
        sp.start();
    }

    private void init() {
        try {
            initLog();
            PropertiesConfiguration properties = initConfiguration();
            List<String> hosts = loadHosts();
            TaskFactory taskFactory = createTaskFactory(properties);
            Reporter reporter = new Reporter(properties);
            TaskExecutionTemplate taskExecutionTemplate = new TaskExecutionTemplate(reporter);
            executionManager = new ExecutionManager(taskExecutionTemplate, taskFactory, properties, hosts);
        } catch (Exception e) {
            log.error("Application failed to load", e);
            System.exit(1);
        }
    }

    private void initLog() {
        File configFile = new File(CONF_LOG4J);
        if (configFile.exists()) {
            Configurator.initialize(null, CONF_LOG4J);
        }
    }

    private void start() {
        executionManager.start();
    }

    private TaskFactory createTaskFactory(PropertiesConfiguration properties) {
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

    private PropertiesConfiguration initConfiguration() throws org.apache.commons.configuration2.ex.ConfigurationException {
        File configFile = new File(CONFIG_PROPERTIES);
        String configFilePath;
        if (configFile.exists()) {
            configFilePath = CONFIG_PROPERTIES;
        } else {
            configFilePath = DEFAULT_CONFIG_PROPERTIES;
        }

        FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                new FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class)
                        .configure(new Parameters().properties()
                                .setFileName(configFilePath)
                                .setThrowExceptionOnMissing(true)
                                .setListDelimiterHandler(new DefaultListDelimiterHandler(';'))
                                .setIncludesAllowed(false));
        return builder.getConfiguration();
    }

    private List<String> loadHosts() throws IOException, URISyntaxException {
        File hostsFile = new File(HOSTS_CSV);
        if (hostsFile.exists()) {
            ;
            return Files.readAllLines(hostsFile.toPath(), StandardCharsets.UTF_8);
        } else {
            try (InputStream resource = this.getClass().getClassLoader().getResourceAsStream(DEFAULT_HOSTS_CSV)) {
                List<String> lines = new BufferedReader(new InputStreamReader(resource,
                        StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
                return lines;
            }
        }
    }
}
