package com.chat.cache;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;

public class OrderCache {
    //单位时间内只允许访问一次
    public static TimedCache<String, Long> times1 = CacheUtil.newTimedCache(2 * 1000);

    public static synchronized boolean moreThanOnce(String key, long off) {
        Long longs = RateLimiterCache.times1.get(key);
        if (longs == null || System.currentTimeMillis() - longs > off * 1000) {
            RateLimiterCache.times1.put(key, (System.currentTimeMillis()));
            return false;
        }
        return true;
    }

}
