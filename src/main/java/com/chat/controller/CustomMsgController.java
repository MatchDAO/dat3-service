package com.chat.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.chat.common.AuthToken;
import com.chat.common.R;
import com.chat.entity.CustomMsg;
import com.chat.entity.User;
import com.chat.service.impl.CustomMsgServiceImpl;
import com.chat.service.impl.UserServiceImpl;
import com.chat.task.queue.MsgDelayTask;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.concurrent.DelayQueue;

/**
 * <p>
 *  自定义消息
 * </p>
 *
 * @author jetBrains
 * @since 2022-12-29
 */
@RestController
@RequestMapping("/customMsg")
public class CustomMsgController {
    @Resource
    private CustomMsgServiceImpl customMsgService;

    @Resource
    private UserServiceImpl userService;
    @Resource
    private DelayQueue<MsgDelayTask> myMsgDelayQueue;
    @AuthToken
    @PostMapping("/sayHi")
    public R sayHi(HashMap<String,String> map ) {
        String msgId = map.getOrDefault("msgId", "");
        String userCode = map.getOrDefault("userCode", "");
        String cuserCode = map.getOrDefault("cuserCode", "");
        if (StrUtil.isEmpty(msgId)|| StrUtil.isEmpty(userCode)|| StrUtil.isEmpty(cuserCode)) {
            return R.error();
        }
        CustomMsg byId = customMsgService.getById(Long.parseLong(msgId));
        if (byId==null) {
            return R.error("no msg");
        }
        User user = userService.getById(userCode);
        if (user==null) {
            return R.error("no u");
        }
        HashMap<String, String> map1 = new HashMap<>();
        map1.put("{name}",user.getUserName());
        String msg=format(byId.getReply(),map1);
        //myMsgDelayQueue.add(new MsgDelayTask(userCode,cuserCode,msg, 1000L *(RandomUtil.randomInt(10,100))));
        return R.success();
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
