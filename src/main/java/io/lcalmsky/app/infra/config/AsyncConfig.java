package io.lcalmsky.app.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // 비동기 처리를 위한 기본 설정을 제공
@Slf4j
public class AsyncConfig implements AsyncConfigurer { // AsyncConfigurer를 구현하여 커스텀 설정을 추가할 수 있다.
    @Override
    public Executor getAsyncExecutor() { // 스레드 풀을 직접 지정
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int processors = Runtime.getRuntime().availableProcessors();
        log.info("processor count {}", processors);
        executor.setCorePoolSize(processors);
        executor.setMaxPoolSize(processors * 2);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("AsyncExecutor");
        executor.initialize();
        return executor;
    }
}
