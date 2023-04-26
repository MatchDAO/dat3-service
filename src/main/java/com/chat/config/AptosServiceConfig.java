package com.chat.config;


import com.chat.common.ChainEnum;
import com.chat.utils.aptos.AptosClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AptosServiceConfig {

    @Bean
    public AptosClient aptosClient() {
        return new AptosClient(ChainEnum.APTOS.getRpcUrl(), info -> {
        }, s -> {
        });
    }
}
