package com.chat.task.queue;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.DelayQueue;

@Configuration
public class MintDelayConfiguration {
    @Bean("myMintTaskQueue")
    public DelayQueue<MintTask> myMsgDelayQueue(){
        return new DelayQueue<MintTask>();
    }
}
