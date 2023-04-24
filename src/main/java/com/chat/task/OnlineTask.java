package com.chat.task;

import com.chat.cache.UserCache;
import com.chat.entity.Creator;
import com.chat.service.impl.CreatorServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class OnlineTask {
    @Resource
    private CreatorServiceImpl creatorService;
    @Resource
    private UserCache userCache;

    @Scheduled(fixedDelay = 25 * 1000)
    public void updateBanner() {
        // todo 缓存
        List<Creator> list = creatorService.list();
        List<Creator> update = new ArrayList<>();
        list.forEach(s -> {
            Long aLong = userCache.userOnlineGet(s.getUserCode());
            if (s.getOnline() == 1) {
                Creator temp = new Creator();
                temp.setLastOnlineTime(LocalDateTime.now());
                temp.setUserCode(s.getUserCode());
                update.add(temp);
                // creatorService.updateById(temp);
            } else if (aLong != 0) {
                Creator temp = new Creator();
                temp.setLastOnlineTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(aLong), ZoneId.systemDefault()));
                temp.setUserCode(s.getUserCode());
                update.add(temp);
                //creatorService.updateById(temp);
            }
        });
        creatorService.updateBatchById(update,100);
    }

}
