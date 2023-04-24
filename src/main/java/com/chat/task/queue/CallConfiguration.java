package com.chat.task.queue;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.DelayQueue;

@Configuration
public class CallConfiguration {
    @Bean("myCallDelayQueue")
    public DelayQueue<CallDelayTask> myCallDelayQueue(){
        return new DelayQueue<CallDelayTask>();
    }
}
