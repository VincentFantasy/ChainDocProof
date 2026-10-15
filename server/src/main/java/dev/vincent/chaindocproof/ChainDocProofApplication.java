package dev.vincent.chaindocproof;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 链上资产存证服务 启动类
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ChainDocProofApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChainDocProofApplication.class, args);
    }
}
