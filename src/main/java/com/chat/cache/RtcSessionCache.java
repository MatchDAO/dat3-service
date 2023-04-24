package com.chat.cache;

import cn.hutool.core.util.StrUtil;
import com.chat.entity.dto.ChannelDto;
import com.chat.utils.RedisUtil;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigInteger;

@Component
public class RtcSessionCache {

    @Resource
    private RedisUtil redisUtil;

    static final String key = "sso:rtc:channel:";

    public ChannelDto channel(String from, String to) {
        Object o = redisUtil.get(key + getChannelName(from,to));
        if (o != null) {
            return (ChannelDto) o;
        }
        return null;
    }

    //新会话
    public ChannelDto newChannel(String from, String to, String frozen,long currentTimeMillis) {
        //发起人请求视频通话 强制初始化通道状态
        if (frozen==null) {
            ChannelDto channelDto = new ChannelDto();
            channelDto.setBegin(currentTimeMillis);
            channelDto.setFrom(from);
            channelDto.setTo(to);
            channelDto.setCurrent(currentTimeMillis);
            channelDto.setFrozen("");
            channelDto.setFrozenTimes(0);
            channelDto.setChannel(getChannelName(from,to));
            channelDto.setReason(0);
            if (redisUtil.set(key + getChannelName(from,to), channelDto, 60 * 60 * 24 * 30)) {
                return channelDto;
            }
        }else {
            //接受人接受call 强制初始化通道状态
            Object o = redisUtil.get(key + getChannelName(from,to));
            if (o != null ) {
                ChannelDto current = (ChannelDto) o;
                ChannelDto channelDto = new ChannelDto();
                channelDto.setBegin(currentTimeMillis);
                channelDto.setFrom(from);
                channelDto.setTo(to);
                channelDto.setCurrent(currentTimeMillis);
                channelDto.setFrozen(frozen + ";");
                channelDto.setFrozenTimes(0);
                channelDto.setReason(0);
                channelDto.setChannel(current.getChannel());
                if (redisUtil.set(key + getChannelName(from,to), channelDto, 60 * 60 * 24 * 30)) {
                    return channelDto;
                }
            }
        }

        return null;
    }
    public ChannelDto newChannelBegin(String from, String to ,long currentTimeMillis) {
        //发起人请求视频通话 强制初始化通道状态

            //接受人接受call 强制初始化通道状态
            Object o = redisUtil.get(key + getChannelName(from,to));
            if (o != null ) {
                ChannelDto current = (ChannelDto) o;
                ChannelDto channelDto = new ChannelDto();
                channelDto.setBegin(currentTimeMillis);
                channelDto.setFrom(from);
                channelDto.setTo(to);
                channelDto.setCurrent(currentTimeMillis);
                channelDto.setFrozen(  "");
                channelDto.setFrozenTimes(1);
                channelDto.setChannel(current.getChannel());
                if (redisUtil.set(key + getChannelName(from,to), channelDto, 60 * 60 * 24 * 30)) {
                    return channelDto;
                }
            }
            else {
                ChannelDto channelDto = new ChannelDto();
                channelDto.setBegin(currentTimeMillis);
                channelDto.setFrom(from);
                channelDto.setTo(to);
                channelDto.setCurrent(currentTimeMillis);
                channelDto.setFrozen("");
                channelDto.setFrozenTimes(1);
                channelDto.setReason(0);
                channelDto.setChannel(getChannelName(from,to));
                if (redisUtil.set(key + getChannelName(from,to), channelDto, 60 * 60 * 24 * 30)) {
                    return channelDto;
                }
            }


        return null;
    }

    //预扣费
    public Boolean channelFrozen(String from, String to, String frozen) {
        Object o = redisUtil.get(key + getChannelName(from,to));
        if (o != null) {
            ChannelDto current = (ChannelDto) o;
            long currentTimeMillis = System.currentTimeMillis();
            current.setFrozenTimes(current.getFrozenTimes() + 1);
            current.setCurrent(currentTimeMillis);
            current.setFrozen(current.getFrozen() + frozen + ";");
            return redisUtil.set(key + getChannelName(from,to), current, 60 * 60 * 24 * 30);
        }
        return false;
    }

    //结束原因 1:from挂断 2:to挂断 3:from资金不足 4未知原因(from/to未知原因中断),5to拒绝
    public Boolean channelEnd(String from, String to, Integer reason) {
        Object o = redisUtil.get(key + getChannelName(from,to));
        if (o != null) {
            ChannelDto current = (ChannelDto) o;
            long currentTimeMillis = System.currentTimeMillis();
            current.setBegin(current.getBegin());
            current.setCurrent(currentTimeMillis);
            current.setReason(reason);
            current.setEnd(currentTimeMillis);
            return redisUtil.set(key + getChannelName(from,to), current, 60 * 60 * 24 * 30);
        }
        return false;
    }
    public static String  getChannelName(String from, String to){
        return new BigInteger(from).subtract(new BigInteger(to)).signum()<1?from+to:to+from;
    }
}
