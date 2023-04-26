package com.chat.task;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.chat.service.AptosService;
import com.chat.task.queue.MintTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.concurrent.DelayQueue;

@Slf4j
@Component
public class MintCoin {

    //    public static void main(String[] args) {
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime endOfDay = now.with(LocalTime.MAX);
//        System.out.println(now);
//        System.out.println(endOfDay);
//    }
    @Resource
    private AptosService aptosService;

    @Resource
    private DelayQueue<MintTask> myMintTaskQueue;

    //@PostConstruct
    public void task1() {
//         res.set("epochInterval", epochInterval);
//         res.set("epoch", epoch);
//         res.set("currentEpochStartTime", currentEpochStartTime);
//         res.set("nextEpochStartTime", nextEpochStartTime);
        JSONArray mint = aptosService.getCoinMint();
        JSONObject epochInfo = aptosService.getEpochInfo();
        log.info("MintCoin {}------{}", mint, epochInfo);
        if (mint != null) {
            Long last_epoch = mint.getLong(2);
            Long epoch = epochInfo.getLong("epoch");
            Long epochInterval = epochInfo.getLong("epochInterval");
            Long nextEpochStartTime = epochInfo.getLong("nextEpochStartTime");
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime max = now.with(LocalTime.MAX);
            if (last_epoch <= 1) {
                Long curr = max.toInstant(ZoneOffset.UTC).toEpochMilli() - System.currentTimeMillis();
                log.info("MintTask  ------{}", curr);
                myMintTaskQueue.add(new MintTask("", "", "", curr, 0, last_epoch));
            } else {
                long pass = epoch - last_epoch;
                if (12 > pass) {
                    // nextEpochStartTime 视为已经过了一个世纪 下次执行时间为last_epoch+12 ==  nextEpochStartTime+(12-pass)*epochInterval
                    Long curr = (nextEpochStartTime + 1) + (epochInterval * (12 - pass - 1)) - System.currentTimeMillis() / 1000;
                    log.info("MintTask  ------{}", curr);
                    myMintTaskQueue.add(new MintTask("", "", "", curr * 1000, 0, last_epoch));

                } else if (12 == pass) {
                    //已经到了mint时间
                    Long curr = 20L;
                    log.info("MintTask  ------{}", curr);
                    myMintTaskQueue.add(new MintTask("", "", "", curr * 1000, 0, last_epoch));
                } else {
                    //已经远远超过mint时间
                    Long curr = max.toInstant(ZoneOffset.UTC).toEpochMilli() - System.currentTimeMillis();
                    log.info("MintTask  ------{}", curr);
                    myMintTaskQueue.add(new MintTask("", "", "", curr, 0, last_epoch));
                }


            }

        }


    }
}
