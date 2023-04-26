package com.chat.task.queue;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.chat.service.AptosService;
import com.chat.utils.aptos.request.v1.model.Response;
import com.chat.utils.aptos.request.v1.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MintTaskRunner implements InitializingBean {
    @Resource
    private DelayQueue<MintTask> myMintTaskQueue;

    @Resource
    private AptosService aptosService;

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            try {
                log.info("MsgDelayTaskRunner {}", myMintTaskQueue.size());
                while (true) {
                    MintTask take = myMintTaskQueue.take();
                    log.info("" + take);
                    try {
                        int done = 0;
                        Response<Transaction> t0 = null;
                        while (done < 10) {
                            JSONArray begin = aptosService.getCoinMint();
                            log.info("MintTaskRunner-{}  begin  {}", done, begin);
                            aptosService.dat3ManagerMintTo();
                            try {
                                TimeUnit.SECONDS.sleep(3);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            //校验是否成功
                            JSONArray now = aptosService.getCoinMint();
                            JSONObject epochInfo = aptosService.getEpochInfo();
                            log.info("MintTaskRunner-{}  now  {}-----{}", done, now, epochInfo);
                            if (begin != null && now != null
                                    && now.getLong(2) != begin.getLong(2)) {
                                Long last_epoch = now.getLong(2);
                                Long epoch = epochInfo.getLong("epoch");
                                Long epochInterval = epochInfo.getLong("epochInterval");
                                Long nextEpochStartTime = epochInfo.getLong("nextEpochStartTime");
                                long curr = nextEpochStartTime + 1 + (epochInterval * 11);
                                if (last_epoch == epoch) {
                                    log.info("MintTaskRunner-{}  begin  {}", done, curr);
                                    myMintTaskQueue.add(new MintTask("", "", "", curr * 1000, 0, last_epoch));
                                    done = 10;
                                }
                            }

                            done++;

                        }
                    } catch (Exception e) {
                        log.error("" + e.fillInStackTrace());
                        e.printStackTrace();
                    }

                }
            } catch (InterruptedException e) {
                // 因为是重写Runnable接口的run方法，子类抛出的异常要小于等于父类的异常。而在Runnable中run方法是没有抛异常的。所以此时是不能抛出InterruptedException异常。如果此时你只是记录日志的话，那么就是一个不负责任的做法，因为在捕获InterruptedException异常的时候自动的将是否请求中断标志置为了false。在捕获了InterruptedException异常之后，如果你什么也不想做，那么就将标志重新置为true，以便栈中更高层的代码能知道中断，并且对中断作出响应。
                Thread.currentThread().interrupt();
            }
        }).start();

    }
}
