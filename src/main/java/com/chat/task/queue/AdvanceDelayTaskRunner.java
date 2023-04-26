package com.chat.task.queue;

import com.chat.cache.RtcSessionCache;
import com.chat.entity.dto.ChannelDto;
import com.chat.service.AgoraService;
import com.chat.service.TransactionUtils;
import com.chat.service.impl.InteractiveServiceImpl;
import com.chat.service.impl.PriceGradeUserServiceImpl;
import com.chat.service.impl.UserServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * discard
 **/

@Slf4j
@Component
public class AdvanceDelayTaskRunner implements InitializingBean {
    @Resource
    private DelayQueue<AdvanceDelayTask> myDelayQueue;
    @Resource
    private RtcSessionCache rtcSessionCache;
    @Resource
    private AgoraService agoraService;

    //结束原因 1:from挂断 2:to挂断 3:from资金不足 4未知原因(from/to未知原因中断),5to拒绝
    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            try {
                log.info("MsgDelayTaskRunner {}", myDelayQueue.size());
                while (true) {
                    AdvanceDelayTask take = myDelayQueue.take();
                    log.info("take:{}", take);
                    log.info("take: {},{},{},{}", take.getChannelName(), take.getDelay(TimeUnit.SECONDS), take.getFrom(), take.getTo());
                    try {
                        //获取用户信息
                        String from = take.getFrom();
                        String to = take.getTo();
                        ChannelDto channel = rtcSessionCache.channel(from, to);
                        agoraService.suspendChannel(channel.getChannel(), 0);
                        //更新通道状态为关闭状态
                        Boolean closed = rtcSessionCache.channelEnd(from, to, take.getFlag());
                        if (!closed) {
                            rtcSessionCache.channelEnd(from, to, take.getFlag());
                        }
                    } catch (Exception e) {
                        log.error(" " + e.fillInStackTrace());
                    }


                }
            } catch (InterruptedException e) {
                // 因为是重写Runnable接口的run方法，子类抛出的异常要小于等于父类的异常。而在Runnable中run方法是没有抛异常的。所以此时是不能抛出InterruptedException异常。如果此时你只是记录日志的话，那么就是一个不负责任的做法，因为在捕获InterruptedException异常的时候自动的将是否请求中断标志置为了false。在捕获了InterruptedException异常之后，如果你什么也不想做，那么就将标志重新置为true，以便栈中更高层的代码能知道中断，并且对中断作出响应。
                Thread.currentThread().interrupt();
            }
        }).start();

    }

    @SneakyThrows
    public static void main(String[] args) {
        long l = System.currentTimeMillis();
        long current = 59;
        new Thread(() -> {
            while (true) {
                System.out.println("            " + (System.currentTimeMillis() - l) / 1000);
                try {
                    TimeUnit.SECONDS.sleep(20);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        TimeUnit.SECONDS.sleep(current);
        while (true) {
            current = (System.currentTimeMillis() - l) / 1000;
            System.out.println(current);
            long temp = current % 60;
            current = (60 - temp) + 2;


            System.out.println(current + "  " + ((System.currentTimeMillis() - l) / 1000) + " " + ((System.currentTimeMillis() - l) / 1000) % 60);
            TimeUnit.SECONDS.sleep(current);
        }

    }
}

