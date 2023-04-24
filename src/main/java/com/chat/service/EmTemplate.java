package com.chat.service;

import cn.hutool.core.collection.CollectionUtil;
import com.easemob.im.server.EMException;
import com.easemob.im.server.EMService;
import com.easemob.im.server.model.EMMetadata;
import com.easemob.im.server.model.EMSentMessageIds;
import com.easemob.im.server.model.EMUser;
import com.easemob.im.server.model.EMUserStatus;
import com.easemob.im.shaded.reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmTemplate {


    @Resource
    private EMService service;

    public EMUser createUser(String userName, String pwd) {
        EMUser user = null;
        try {
            user = service.user().create(userName, pwd).block();
        } catch (EMException e) {
            log.error(e.getMessage());
        }
        return user;
    }

    public EMUser getUser(String userName) {
        EMUser user = null;
        try {
            user = service.user().get(userName).block();
        } catch (EMException e) {
            e.getErrorCode();
            e.getMessage();
        }
        return user;
    }


    public List<EMUserStatus> isUsersOnline(List<String> list) {
        List<EMUserStatus> list1 = new ArrayList<>();
        if (!CollectionUtils.isEmpty(list)) {
            int forNum = list.size() / 100;
            //循环每次插入100条
            for (int f = 0; f < forNum; f++) {
                list1.addAll(isUsersOnline100(CollectionUtil.sub(list, f * 100, (f + 1) * 100)));
            }
            //插入剩余的部分
            if (list.size() % 100 > 0) {
                list1.addAll(isUsersOnline100(CollectionUtil.sub(list, forNum * 100, list.size())));
            }
        }

        return list1;
    }

    public List<EMUserStatus> isUsersOnline100(List<String> userNames) {
        if (CollectionUtils.isEmpty(userNames) || userNames.size() > 100) {
            log.error("The number of users must be less than 100 ");
            return null;
        }
        List<EMUserStatus> list = null;
        try {
            list = service.user().isUsersOnline(userNames).block();
        } catch (EMException e) {
            e.getErrorCode();
            e.getMessage();
        }
        return list;
    }

    public boolean contactAdd(String userName, String contact) {
        try {
            service.contact().add(userName, contact).block();
        } catch (EMException e) {
            e.getErrorCode();
            e.getMessage();
            return false;
        }
        return true;
    }

    public List<String> contactList(String userName) {
        List<String> list = null;
        try {
            list = service.contact().list(userName).collectList().block();
        } catch (EMException e) {
            e.getErrorCode();
            e.getMessage();
        }
        return list;
    }

    //设置用户属性
    public void setMetadataToUser(String userName, Map map) {

        try {
            service.metadata().setMetadataToUser(userName, map).subscribe();
        } catch (EMException e) {
            e.getErrorCode();
            e.getMessage();
        }
    }

    public Map<String, String> getMetadataToUser(String userName) {

        try {
            EMMetadata block = service.metadata().getMetadataFromUser(userName).block();
            return block.getData();
        } catch (EMException e) {
            e.getErrorCode();
            e.getMessage();
        }
        return null;
    }

    //删除用户
    public void deleteUser(String userName, String pwd) {
        EMUser user = null;
        try {
            service.user().delete(userName).block();
        } catch (EMException e) {
            e.getErrorCode();
            e.getMessage();
        }
    }

    //获取token
    public String getUserToken(String userName, String uuid, Integer expire, String pwd) {

        String token = "";
        try {
            EMUser emUser = new EMUser(userName, uuid, true, null);
            token = service.token().getUserToken(emUser, null, null, pwd);
        } catch (EMException e) {
            e.getErrorCode();
            e.getMessage();
        }
        return token;
    }


    //发送消息
    public void messageSend(String fromUser, String toUser, String msgStr) {
        long l = System.currentTimeMillis();
        try {
            Mono<EMSentMessageIds> send = service.message().send()
                    .fromUser(fromUser).toUser(toUser)
                    .text(msg -> msg.text(msgStr))
                    .syncDevice(true)
                    .send();
            send.subscribe((s) -> s.getMessageIdsByEntityId().forEach((k, v) -> {
                log.info((System.currentTimeMillis() - l) + "getMessageIdsByEntityId :: {},{}", k, v);
            }));
//            send.subscribe((s)-> s.getMessageIdsByEntityId().forEach((k, v) -> {
//               log.info((System.currentTimeMillis()-l)+ "getMessageIdsByEntityId :: {},{}", k, v);
//           }));

//            block.getMessageIdsByEntityId().forEach((k, v) -> {
//                log.info("getMessageIdsByEntityId :: {},{}", k, v);
//            });
        } catch (EMException e) {
            e.getErrorCode();
            e.getMessage();
        }

    }

    //更新推送消息名字
    public void updateUserNickname(String userName, String nickname) {
        try {
            service.push().updateUserNickname(userName, nickname).block();
        } catch (EMException e) {
            e.getErrorCode();
            e.getMessage();
        }


    }

    public String getHistoryAsUri() {
        try {
            return service.message().getHistoryAsUri(Instant.now().minusSeconds(3600 * 2)).block();
        } catch (EMException e) {
            e.getErrorCode();
            e.getMessage();
            e.printStackTrace();
        }
        return null;

    }

}
