package com.chat.config;


import com.chat.config.own.PrivateConfig;
import com.easemob.im.server.EMProperties;
import com.easemob.im.server.EMService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EMServiceConfig {
    @Bean
    public EMService eMService() {
//        EMProperties properties = EMProperties.builder()
//                .setAppkey("1155221018099186#demo")
//                .setClientId("YXA61a7UiAdqRNqbzVgoYAe2dw")
//                .setClientSecret("YXA6_QauYrWuf9F4Z62QIbqyPlMWmtA")
//                .build();
        EMProperties properties = EMProperties.builder()
                .setAppkey(PrivateConfig.EMS_APP_KEY)
                .setClientId(PrivateConfig.EMS_CLIENT_ID)
                .setClientSecret(PrivateConfig.EMS_CLIENT_SECRET)
                .turnOffUserNameValidation()
                .build();

        return new EMService(properties);
    }
}
