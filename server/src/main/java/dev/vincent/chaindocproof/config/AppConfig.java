package dev.vincent.chaindocproof.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通用 Bean 配置
 */
@Configuration
public class AppConfig {

    /** 系统时钟（服务内统一走该 Bean，便于单元测试替换） */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
