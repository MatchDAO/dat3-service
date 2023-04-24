package com.chat.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.chat.entity.Creator;
import com.chat.entity.CustomMsg;
import com.chat.entity.User;
import com.chat.entity.dto.CreatorDto;
import com.chat.mapper.CustomMsgMapper;
import com.chat.service.CustomMsgService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jetBrains
 * @since 2022-12-29
 */
@Service
public class CustomMsgServiceImpl extends ServiceImpl<CustomMsgMapper, CustomMsg> implements CustomMsgService {
    @Resource
    private CreatorServiceImpl creatorService;


    public List<HashMap<String, String>> getCustomMsg(String userCode,String userName){
        //获取随机人数
        int i = RandomUtil.randomInt(1, 5);
        MPJLambdaWrapper<Creator> wrapper = new MPJLambdaWrapper<Creator>()
                .selectAll(Creator.class)
                .select(User::getUserName, User::getPortrait, User::getBio)
                .leftJoin(User.class, User::getUserCode, Creator::getUserCode)
                .eq(Creator::getOnline, 1);
        List<CreatorDto> list = creatorService.selectJoinList(CreatorDto.class, wrapper);
        List<CustomMsg> listMsg = this.list();
        HashMap<Integer, CreatorDto> creatorMap = new HashMap<>();
        HashMap<Integer, CustomMsg> customMsgMap = new HashMap<>();
        for (int i1 = 0; i1 < i; i1++) {
            int i2 = RandomUtil.randomInt(0, list.size()-1);
            int i3 = RandomUtil.randomInt(0, listMsg.size()-1);
            creatorMap.put(i2,list.get(i2)) ;
            customMsgMap.put(i2,listMsg.get(i3));
        }

        List sayHai=new ArrayList();
        creatorMap.forEach((key,cr)->{
            HashMap<String, String> customMsg = new HashMap<>();
            customMsg.put("userCode",userCode);
            customMsg.put("userName",userName);
            customMsg.put("cuserCode",cr.getUserCode());
            customMsg.put("cuserName",cr.getUserName());
            HashMap<String, String> tar=new HashMap<>();
            tar.put("{name}",cr.getUserName());
            CustomMsg customMsg1 = customMsgMap.get(key);
            customMsg.put("msgId",""+customMsg1.getId());
            customMsg.put("sayHi",format(customMsg1.getSayHi(),tar));
            customMsg.put("reply",format(customMsg1.getReply(),tar));
            sayHai.add(customMsg);
        });
        return sayHai;
    }

    public String format(String msg,HashMap<String, String> tar ){
        final String[] msg1 = {msg};
        tar.forEach((key,str)->{
            if (msg1[0].contains(key)) {
                msg1[0] = msg1[0].replace(key, str);
            }
        });
        return msg1[0];
    }
}
