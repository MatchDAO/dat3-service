package com.chat.cache;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import com.chat.common.RedisKeys;
import com.chat.config.ChatConfig;
import com.chat.entity.User;
import com.chat.entity.ValidationFailed;
import com.chat.utils.RedisUtil;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class UserCache {
    @Resource
    private RedisUtil redisUtil;

    //用户缓存
    private static final Map<String, User> ALL_USER_CACHE = new ConcurrentHashMap<>();
    //在线用户
    private static TimedCache<String, Long> ONLINE_USER = CacheUtil.newTimedCache(60 * 1000);
    //每天验证失败10次不允许登陆
    private static Cache<String, ValidationFailed> VALIDATION_FAILED = CacheUtil.newLFUCache(ChatConfig.VALIDATION_FAILED_CACHE_CAPACITY, 60 * 60 * 24 * 1000);
    private static Cache<String, String> INTERNAL_ACCOUNTS = CacheUtil.newLFUCache(500);


    public void userOnline(String userKey) {
        ONLINE_USER.put(userKey, System.currentTimeMillis());
        redisUtil.hset(RedisKeys.ONLINE_USER, userKey, System.currentTimeMillis());
    }

    public long userOnlineGet(String userKey) {
        Long aLong = ONLINE_USER.get(userKey, false, () -> 0L);
        if (aLong == 0) {
            Object hget = redisUtil.hget(RedisKeys.ONLINE_USER, userKey);
            if (hget != null) {
                aLong = (Long) hget;
            }
        }
        return aLong;
    }

    public void userOnlineDel(String userKey) {
        ONLINE_USER.remove(userKey);
    }

    public Long validationFailed(String key) {
        ValidationFailed validationFailed = VALIDATION_FAILED.get(key);
        return validationFailed == null ? 0L : validationFailed.getTimes();
    }

    public void addValidationFailed(String key) {
        ValidationFailed validationFailed = VALIDATION_FAILED.get(key, false, () -> new ValidationFailed(0L, System.currentTimeMillis()));
        validationFailed.setTimes(validationFailed.getTimes() + 1);
        VALIDATION_FAILED.put(key, validationFailed, (validationFailed.getTimes() + (60 * 60 * 5 * 1000)) - System.currentTimeMillis());
    }

    public long addAndValidationFailedTimes(String key) {
        ValidationFailed validationFailed = VALIDATION_FAILED.get(key, false, () -> new ValidationFailed(0L, System.currentTimeMillis()));
        validationFailed.setTimes(validationFailed.getTimes() + 1);
        VALIDATION_FAILED.put(key, validationFailed, (validationFailed.getTimes() + (60 * 60 * 5 * 1000)) - System.currentTimeMillis());
        return validationFailed.getTimes();
    }


    public void AddOneUser(String userId, User user) {
        if (userId != null && user != null) {
            ALL_USER_CACHE.put(userId, user);
        }
    }

    public User getUser(String userId) {
        return ALL_USER_CACHE.get(userId);
    }
    public Map<String, User> getAllUser( ) {
        return ALL_USER_CACHE ;
    }
    public Boolean containsUser(String userId) {
        return ALL_USER_CACHE.containsKey(userId);
    }

    public boolean isInternal(String key) {
        return INTERNAL_ACCOUNTS.containsKey(key);
    }

    public void addInternal(String key, String v) {
        INTERNAL_ACCOUNTS.put(key, v);

    }
    public Object getByAddress(String address) {
       return  redisUtil.hget("sso:user:address", address);
    }
    public void AddByAddress(String address,User user) {
        redisUtil.hset("sso:user:address", address,user);
    }

}
