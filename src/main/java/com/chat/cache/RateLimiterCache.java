package com.chat.cache;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollUtil;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class RateLimiterCache {
    //单位时间内只允许访问一次
    public static TimedCache<String, Long> times1 = CacheUtil.newTimedCache(80 * 1000);
    //限频
    public static TimedCache<String, LinkedList<Long>> RATE_LIMITER = CacheUtil.newTimedCache(60 * 1000);

    /**
     * 限流器
     * 单位时间内限制可访问多少次
     * 每一次请求会被记录下来 判断单位时间内访问次数是否超过限制
     *
     * @param key       限流器的key
     * @param times     timeLimit时间内 可访问的次数
     * @param timeLimit 单位时间  单位毫秒
     * @return
     */


    public static synchronized boolean overLimiter(String key, long times, long timeLimit) {
        LinkedList<Long> longs = RateLimiterCache.RATE_LIMITER.get(key);
        System.out.println(longs == null ? null : longs.size());
        if (CollUtil.isEmpty(longs)) {
            LinkedList<Long> newLong = new LinkedList<>();
            //第一次请求
            newLong.add(System.currentTimeMillis());
            RateLimiterCache.RATE_LIMITER.put(key, newLong, timeLimit * 2);
            return false;
        }
        //移除过期记录
        longs.removeIf(s -> (System.currentTimeMillis() - s) > timeLimit);
        // todo 记录每次请求->1110000000 111 or 只记录成功的请求(限流器)->111000100010001001

        //RateLimiterCache.RATE_LIMITER.put(key, longs,timeLimit*2);
        //请求次数没有超限
        if (longs.size() <= times) {
            longs.add(System.currentTimeMillis());
            return false;
        }
        //防止短时间内访问过多内存膨胀
        if (longs.size() - times > 3) {
            longs.removeFirst();
        }

        return true;
    }

    /**
     * @param key
     * @param off
     * @return boolean  true:已经超过一次,应该限流   false:第一次访问 不需要限流
     */
    public static synchronized boolean moreThanOnce(String key, long off) {
        Long longs = RateLimiterCache.times1.get(key);
        if (longs == null || System.currentTimeMillis() - longs > off * 1000) {
            RateLimiterCache.times1.put(key, (System.currentTimeMillis()));
            return false;
        }
        return true;
    }


    public static synchronized boolean moreThanOnce(String key, long off, long timeout) {
        Long longs = RateLimiterCache.times1.get(key,false);
        if (longs == null || System.currentTimeMillis() - longs < off * 1000) {
            RateLimiterCache.times1.put(key, (System.currentTimeMillis()), off * 1000);
            return false;
        }
        return true;
    }

    private static volatile boolean flag = true;


    public static void main1(String[] args) throws InterruptedException {
        long l = System.currentTimeMillis();
        for (int j = 1; j < 4; j++) {
            for (int i = 0; i < 10; i++) {
                int finalI = i;
                int finalJ = j;
                new Thread(() -> {
                    while (flag) {
                        System.out.println(finalJ + "t" + "" + finalI + "" +
                                "," + moreThanOnce("t" + finalJ + "" + finalI, finalJ) + " " + (System.currentTimeMillis() - 1662655300000L));
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }, "t" + finalI).start();
            }
        }
        for (int i = 0; i < 32; i++) {
            Thread.sleep(1000);
        }
        flag = false;
        System.out.println("===========================================================================" + (System.currentTimeMillis() - l));
        System.out.println(times1);
    }

}
