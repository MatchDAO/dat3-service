package com.chat.task.queue;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
public class AdvanceDelayTask implements Delayed {


    private Long begin;
    private String from;
    private String to;
    private String channelName;
    private Integer flag;
    private Long expire;


    public AdvanceDelayTask(String from, String to, String channelName, long expire,Integer flag ,Long begin ) {
        this.from=from;
        this.begin=begin;
        this.to=to;
        this.flag=flag;
        this.channelName=channelName;
        this.expire = expire + System.currentTimeMillis();
    }

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        long remaining = expire - System.currentTimeMillis();
        return unit.convert(remaining, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return (int)(this.getDelay(TimeUnit.MILLISECONDS)-o.getDelay(TimeUnit.MILLISECONDS))  ;
    }


}
