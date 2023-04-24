package com.chat.task.queue;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.DelayQueue;

@Configuration
public class AdvanceConfiguration {
    @Bean("myAdvanceDelayQueue")
    public DelayQueue<AdvanceDelayTask> myAdvanceDelayQueue(){
        return new DelayQueue<AdvanceDelayTask>();
    }
}
