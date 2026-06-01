package org.xxg.backend.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Spring Boot应用入口类
 * 启动后端服务，启用异步任务（@EnableAsync）和定时任务（@EnableScheduling）支持，
 * 并配置全局异步任务线程池。
 */
@SpringBootApplication
@EnableAsync    // 启用异步方法执行支持
@EnableScheduling  // 启用定时任务调度支持
public class BackendApplication {

    /**
     * 应用主入口方法
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    /**
     * 配置异步任务线程池
     * 核心线程5个，最大线程20个，队列容量100，超出时拒绝策略默认AbortPolicy
     * @return 线程池执行器
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);      // 核心线程数
        executor.setMaxPoolSize(20);      // 最大线程数
        executor.setQueueCapacity(100);   // 等待队列容量
        executor.setThreadNamePrefix("async-"); // 线程名前缀，便于日志排查
        executor.initialize();
        return executor;
    }
}
