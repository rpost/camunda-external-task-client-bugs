package com.example.workflow;

import com.sun.management.OperatingSystemMXBean;
import org.assertj.core.api.Assertions;
import org.camunda.bpm.client.ExternalTaskClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ApplicationTest {

    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @Test
    void shouldStopExternalTaskClientWithoutWaitingFor1Minute() throws InterruptedException {
        ExternalTaskClient client = ExternalTaskClient.create()
                .baseUrl("http://localhost:8080/engine-rest/")
                .asyncResponseTimeout(60_000)
                .disableBackoffStrategy()
                .build();

        client.subscribe("sampleTopic")
                .handler((externalTask, externalTaskService) -> externalTaskService.complete(externalTask))
                .open();

        client.stop();
    }

    @Test
    void shouldNotOverheatCpuWhenNoTopicsToSubscribe() throws InterruptedException {
        ExternalTaskClient client = ExternalTaskClient.create()
                .baseUrl("http://localhost:8080/engine-rest/")
                .build();

        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        long startCpuUsage = osBean.getProcessCpuTime();
        Thread.sleep(5_000);
        long endCpuUsage = osBean.getProcessCpuTime();
        long cpuUsage = endCpuUsage - startCpuUsage;
        Assertions.assertThat(cpuUsage).isLessThan(1_000_000_000); // 1s = 1_000_000_000ns
    }
}