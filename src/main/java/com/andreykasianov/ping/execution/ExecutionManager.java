package com.andreykasianov.ping.execution;

import com.andreykasianov.ping.task.PingTask;
import com.andreykasianov.ping.task.PingTaskType;
import com.andreykasianov.ping.task.TaskFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.PropertiesConfiguration;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ExecutionManager creates ping tasks for all hosts, schedules ping tasks
 */

@Slf4j
public class ExecutionManager {
    private Map<PingTaskType, List<PingTask>> taskPool = new HashMap<>();
    private PropertiesConfiguration properties;
    private ThreadPoolExecutor icmpThreadPoolExecutor;
    private ThreadPoolExecutor httpThreadPoolExecutor;
    private ThreadPoolExecutor traceThreadPoolExecutor;
    private Timer icmpScheduler = new Timer("icmpScheduler", false);
    private Timer httpScheduler = new Timer("httpScheduler", false);
    private Timer traceScheduler = new Timer("traceScheduler", false);
    private TaskExecutionTemplate taskExecutionTemplate;
    private TaskFactory taskFactory;


    public ExecutionManager(TaskExecutionTemplate taskExecutionTemplate, TaskFactory taskFactory, PropertiesConfiguration properties, List<String> hosts) {
        this.taskExecutionTemplate = taskExecutionTemplate;
        this.taskFactory = taskFactory;
        this.properties = properties;
        int poolSize = hosts.size() * 2;
        icmpThreadPoolExecutor = createExecutorPool(poolSize);
        traceThreadPoolExecutor = createExecutorPool(poolSize);
        httpThreadPoolExecutor = createExecutorPool(poolSize);

        initPingTaskPool(hosts);
    }

    /***
     * Creates ping tasks for all hosts and stores them in pool
     * @param hosts - hosts to ping
     */
    private void initPingTaskPool(List<String> hosts) {
        List<PingTask> icmpPingTasks = hosts
                .stream()
                .map(host -> taskFactory.getTask(PingTaskType.ICMP, host))
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .collect(Collectors.toList());

        List<PingTask> tracePingTasks = hosts
                .stream()
                .map(host -> taskFactory.getTask(PingTaskType.TRACE, host))
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .collect(Collectors.toList());

        List<PingTask> httpPingTasks = hosts
                .stream()
                .map(host -> taskFactory.getTask(PingTaskType.HTTP, host))
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .collect(Collectors.toList());

        taskPool.put(PingTaskType.ICMP, icmpPingTasks);
        taskPool.put(PingTaskType.TRACE, tracePingTasks);
        taskPool.put(PingTaskType.HTTP, httpPingTasks);
    }

    /***
     * Initiates ping scheduling
     */
    public void start() {
        long icmpInterval = properties.getLong("icmp.interval.milliseconds", 60_000);
        long traceInterval = properties.getLong("trace.interval.milliseconds", 60_000);
        long httpInterval = properties.getLong("http.interval.milliseconds", 60_000);

        icmpScheduler.scheduleAtFixedRate(getTaskRunner(PingTaskType.ICMP, icmpThreadPoolExecutor), 0, icmpInterval);
        traceScheduler.scheduleAtFixedRate(getTaskRunner(PingTaskType.TRACE, traceThreadPoolExecutor), 0, traceInterval);
        httpScheduler.scheduleAtFixedRate(getTaskRunner(PingTaskType.HTTP, httpThreadPoolExecutor), 0, httpInterval);
    }

    /***
     * Stops ping scheduling
     */
    public void stop(){
        icmpScheduler.cancel();
        traceScheduler.cancel();
        httpScheduler.cancel();
        icmpThreadPoolExecutor.shutdown();
        httpThreadPoolExecutor.shutdown();
        traceThreadPoolExecutor.shutdown();
    }

    /***
     * Prepares TimerTask for the given ping task type.
     * It will execute ping tasks of the given type for all hosts in specified thread pool.
     * @param taskType - ping task type
     * @param executor - thread pool for the give ping task type
     * @return
     */
    private TimerTask getTaskRunner(PingTaskType taskType, ThreadPoolExecutor executor) {
        return new TimerTask() {
            @Override
            public void run() {
                try {
                    taskPool.get(taskType)
                            .forEach(task -> executor.execute(() -> taskExecutionTemplate.run(task)));
                } catch (Exception e) {
                    log.error("");
                }
            }
        };
    }

    /**
     * Creates executor pool of the given size
     * @param size
     * @return
     */
    private ThreadPoolExecutor createExecutorPool(int size) {
        return new ThreadPoolExecutor(size,
                size, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                createDaemonThreadFactory());
    }

    /***
     * Thread factory for executor pool
     * @return
     */
    public static ThreadFactory createDaemonThreadFactory() {
        return r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        };
    }
}
