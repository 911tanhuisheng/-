package com.spingbootinit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步判题线程池：提交接口快速返回，判题在后台执行。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String JUDGE_TASK_EXECUTOR = "judgeTaskExecutor";

    public static final String BAILIAN_ASSIST_EXECUTOR = "bailianAssistExecutor";

    @Bean(name = JUDGE_TASK_EXECUTOR)
    public Executor judgeTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("judge-async-");
        executor.initialize();
        return executor;
    }

    /** 百炼 SSE 等阻塞 IO，与判题池隔离 */
    @Bean(name = BAILIAN_ASSIST_EXECUTOR)
    public Executor bailianAssistExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("bailian-assist-");
        executor.initialize();
        return executor;
    }
}
