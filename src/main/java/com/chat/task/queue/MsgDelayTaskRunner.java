package com.chat.task.queue;

import com.chat.service.EmTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MsgDelayTaskRunner implements InitializingBean {
    @Resource
    private DelayQueue<MsgDelayTask> myMsgDelayQueue;
    @Resource
    private EmTemplate emTemplate;
    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            try {
                log.info("MsgDelayTaskRunner {}",myMsgDelayQueue.size());
                while(true) {
                    MsgDelayTask take = myMsgDelayQueue.take();
                    //MsgDelayTaskRunner:26 - hello there handsome, tell me something interesting about yourself that most people don’t know.,0,21110821,21110808
                    //2023-01-09 08:31:05,183  INFO EmTemplate:154 - 21110808,1098172736733190760
                    log.info("{},{},{},{}",take.getMsg(),take.getDelay(TimeUnit.SECONDS),take.getCuserCode(),take.getUserCode());
                    // 当队列为null的时候，poll()方法会直接返回null, 不会抛出异常，但是take()方法会一直等待，因此会抛出一个InterruptedException类型的异常。(当阻塞方法收到中断请求的时候就会抛出InterruptedException异常)
                   // emTemplate.messageSend(take.getCuserCode(),take.getUserCode(),take.getMsg());
                    // 执行业务
                }
            } catch (InterruptedException e) {
                // 因为是重写Runnable接口的run方法，子类抛出的异常要小于等于父类的异常。而在Runnable中run方法是没有抛异常的。所以此时是不能抛出InterruptedException异常。如果此时你只是记录日志的话，那么就是一个不负责任的做法，因为在捕获InterruptedException异常的时候自动的将是否请求中断标志置为了false。在捕获了InterruptedException异常之后，如果你什么也不想做，那么就将标志重新置为true，以便栈中更高层的代码能知道中断，并且对中断作出响应。
                Thread.currentThread().interrupt();
            }
        }).start();

    }
}
