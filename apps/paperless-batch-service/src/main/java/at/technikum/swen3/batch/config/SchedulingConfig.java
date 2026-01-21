package at.technikum.swen3.batch.config;

import at.technikum.swen3.batch.service.BatchImportService;
import java.time.ZonedDateTime;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

@Configuration
public class SchedulingConfig implements SchedulingConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulingConfig.class);

    private final BatchProperties batchProperties;
    private final BatchImportService batchImportService;

    public SchedulingConfig(BatchProperties batchProperties, BatchImportService batchImportService) {
        this.batchProperties = batchProperties;
        this.batchImportService = batchImportService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskScheduler());
        taskRegistrar.addTriggerTask(batchImportService::runImportBatch, nextExecutionTrigger());
    }

    private Trigger nextExecutionTrigger() {
        return new Trigger() {
            @Override
            public java.time.Instant nextExecution(TriggerContext triggerContext) {
                ZonedDateTime now = ZonedDateTime.now(batchProperties.zoneId());
                ZonedDateTime next = now.withHour(batchProperties.runAt().getHour())
                        .withMinute(batchProperties.runAt().getMinute())
                        .withSecond(0)
                        .withNano(0);
                if (!next.isAfter(now)) {
                    next = next.plusDays(1);
                }
                LOG.info("Next batch execution scheduled for {}", next);
                return next.toInstant();
            }
        };
    }

    @Bean(destroyMethod = "shutdown")
    public Executor taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("batch-import-");
        scheduler.initialize();
        return scheduler;
    }
}
