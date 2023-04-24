package com.chat.task;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chat.common.InteractiveStautsEnum;
import com.chat.entity.Creator;
import com.chat.entity.Interactive;
import com.chat.entity.dto.WalletUserResult;
import com.chat.service.TransactionUtils;
import com.chat.service.impl.CreatorServiceImpl;
import com.chat.service.impl.InteractiveServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class NoReplyTask {
    @Resource
    private CreatorServiceImpl creatorService;
    @Resource
    private TransactionUtils transactionUtils;
    @Resource
    private InteractiveServiceImpl interactiveService;

   // @Scheduled(fixedDelay = 300 * 1000)
    public void updateNoReply() {
        List<Creator> update = new ArrayList<>();
        //获取所有未回复的超时的消息
        List<Interactive> noReply = interactiveService.list(new LambdaQueryWrapper<Interactive>()
                .eq(Interactive::getStatus,InteractiveStautsEnum.SEND.getType())
                .le(Interactive::getCreateTime, LocalDateTime.now().plusHours(-12).plusMinutes(-5)));
        List<String> noReplyIds=new ArrayList<>();
        if (!CollUtil.isEmpty(noReply)) {
            log.info("noReply count:{}",noReply);
            Map<String, List<Interactive>> creatorMap = noReply.stream().collect(Collectors.groupingBy(Interactive::getCreator));
            //creator分组
            creatorMap.forEach((to, v1) -> {
                if (!CollUtil.isEmpty(v1)) {
                    //user分组
                    Map<String, List<Interactive>> user = v1.stream().collect(Collectors.groupingBy(Interactive::getUserCode));
                    user.forEach((from,v2)->{
                        try {
                            for (Interactive interactive : v2) {
                                noReplyIds.add(interactive.getId());
                            }
//                            WalletUserResult walletUserResult = transactionUtils.noReply(from, to);
//                            if (walletUserResult.getCode()==200) {
//
//                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }

            });
        }

        interactiveService.updateInteractiveStauts(InteractiveStautsEnum.OVERTIME.getType(),noReplyIds);
    }

}
