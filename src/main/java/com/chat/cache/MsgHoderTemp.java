package com.chat.cache;

import com.chat.service.AptosService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MsgHoderTemp {
    @Resource
    private AptosService aptosService;
    Cache<String, Vector<Long>> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .removalListener((String key, Vector<Long> value, RemovalCause cause) -> {
                log.info("MsgHoderTemp:{},{},{}", key, value, cause);
                if (cause.equals(RemovalCause.EXPIRED )&& value.size()>0) {
                    String[] split = key.split("@");
                    aptosService.dat3_routel_send_msg(split[0], split[1], value.size(),split[2]);
                }
            })
            .scheduler(Scheduler.forScheduledExecutorService(Executors.newScheduledThreadPool(1)))
            .maximumSize(10000)
            .build();


    public void addMsgHoder(String userKey) {
        Vector<Long> longs = getMsgHoder(userKey);
        longs.add(System.currentTimeMillis());
        cache.put(userKey,longs);
    }
    public void addMsgHoderExpiredNow(String userKey) {
        Vector<Long> longs = getMsgHoder(userKey);
        longs.add(System.currentTimeMillis());
        cache.put(userKey,longs);
    }

    public Vector<Long> getMsgHoder(String userKey) {
        return cache.get(userKey, k -> new Vector<>());
    }
    public Vector<Long> getIfPresent(String userKey) {
        return cache.getIfPresent(userKey);
    }

}
