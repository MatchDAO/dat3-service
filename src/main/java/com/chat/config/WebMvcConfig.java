package com.chat.config;

import com.chat.config.filter.AuthorizationInterceptor;
import com.chat.config.filter.MyLocaleResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Resource
    private AuthorizationInterceptor authorizationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor)
        //注释掉->默认拦截所有   .addPathPatterns("/**")
        ;
    }
    @Bean
    public LocaleResolver localeResolver(){
        return new MyLocaleResolver();
    }




}
