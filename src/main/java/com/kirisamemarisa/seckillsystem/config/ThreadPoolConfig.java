package com.kirisamemarisa.seckillsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池。当前仅给商品缓存「延迟双删」使用。
 *
 * <p>{@code CallerRunsPolicy}：队列满时回退到调用线程执行，避免丢删除任务导致本地/Redis 脏缓存长期残留。
 * 停机等待任务跑完，防止进程退出时第二次删除被中断。无外部中间件、无 {@code @Order}。
 */
@Configuration
@EnableAsync

public class ThreadPoolConfig {
    /**
     * 延迟双删工作线程池，Bean 名供 {@code @Qualifier("doubleDeleteExecutor")} 注入。
     *
     * @return 小核心、有界队列的任务执行器
     */
    @Bean("doubleDeleteExecutor")
    public Executor doubleDeleteExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("DoubleDelete-Worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
