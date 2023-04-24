package com.chat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 放行白名单配置
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "base")
public class TransactionConfig {

    private String transactionIp;
    private String transactionUrl;

}
