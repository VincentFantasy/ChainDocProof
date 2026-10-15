package dev.vincent.chaindocproof.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

/**
 * web3j 配置：按配置的 RPC 地址构建 Web3j 客户端
 */
@Configuration
public class Web3jConfig {

    @Bean
    public Web3j web3j(ChainProperties chain) {
        HttpService httpService = new HttpService(chain.getRpcUrl());
        return Web3j.build(httpService);
    }
}
