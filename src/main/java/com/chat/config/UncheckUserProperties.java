package com.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 放行白名单配置
 */
@Configuration
@ConfigurationProperties(prefix = "security.uncheckusercode")
public class UncheckUserProperties {

    private List<String> whites = new ArrayList<>();

    public List<String> getWhites() {
        return whites;
    }

    public void setWhites(List<String> whites) {
        this.whites = whites;
    }
}
