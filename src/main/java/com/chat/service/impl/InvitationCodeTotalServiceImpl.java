package com.chat.service.impl;

import com.chat.common.ActionEnum;
import com.chat.common.RedisKeys;
import com.chat.entity.InvitationCodeTotal;
import com.chat.mapper.InvitationCodeTotalMapper;
import com.chat.service.InvitationCodeTotalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.utils.DateUtils;
import com.chat.utils.RedisUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jetBrains
 * @since 2022-12-21
 */
@Service
public class InvitationCodeTotalServiceImpl extends ServiceImpl<InvitationCodeTotalMapper, InvitationCodeTotal> implements InvitationCodeTotalService {

    @Resource
    private RedisUtil redisUtil;
    @Override
    public Integer changeTotal(String action, Integer change, String userCode,int used ) {

        InvitationCodeTotal byId = this.getById(userCode);
        if (byId == null) {
            byId = new InvitationCodeTotal();
            byId.setUsed(0);
            byId.setUserCode(userCode);
            byId.setTotal(ActionEnum.SIGN_IN.getAdd());
            save(byId);
            redisUtil.hset(RedisKeys.USER+":inv:l"+ DateUtils.getSimpleToday(),userCode,1);
            return change;
        }
        if (used>0) {
            byId.setUsed(byId.getUsed()+used);
            updateById(byId);
            return byId.getTotal();
        }
        ActionEnum of = ActionEnum.of(action);
        //登陆赠送
        if (ActionEnum.SIGN_IN.equals(of)) {
            if (redisUtil.hHasKey(RedisKeys.USER+":inv:l"+ DateUtils.getSimpleToday(),userCode)) {
               return byId.getTotal();
            }
             String key=RedisKeys.USER+":inv:l"+ DateUtils.getSimpleToday();
            redisUtil.hset(key,userCode,1);
            redisUtil.hSetExpire(key,1L, TimeUnit.DAYS);
            change=of.getAdd();
        }
        //聊天赠送
        if (ActionEnum.SEND_MSG.equals(of)) {
            long hincr = redisUtil.hincr(RedisKeys.USER + ":inv:s" + DateUtils.getSimpleToday(), userCode, 1);
            redisUtil.hSetExpire(RedisKeys.USER + ":inv:s" + DateUtils.getSimpleToday(),1L, TimeUnit.DAYS);
            if (hincr>50) {
                return byId.getTotal();
            }
            change=of.getAdd();
        }

        byId.setTotal(byId.getTotal() + change);
        updateById(byId);
        return byId.getTotal();
    }
}
