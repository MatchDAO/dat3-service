package com.chat.task.queue;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
public class MsgDelayTask implements Delayed {

    private String userCode;
    private String cuserCode;
    private String msg;
    private Long expire;


    public MsgDelayTask(String userCode, String cuserCode, String msg,long expire  ) {
        this.msg=msg;
        this.userCode=userCode;
        this.cuserCode=cuserCode;
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
