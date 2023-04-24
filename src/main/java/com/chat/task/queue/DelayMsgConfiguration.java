package com.chat.task.queue;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.DelayQueue;

@Configuration
public class DelayMsgConfiguration {
    @Bean("myMsgDelayQueue")
    public DelayQueue<MsgDelayTask> myMsgDelayQueue(){
        return new DelayQueue<MsgDelayTask>();
    }
}
