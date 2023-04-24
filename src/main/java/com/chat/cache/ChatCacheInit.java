package com.chat.cache;


import cn.hutool.core.util.StrUtil;
import com.chat.entity.Creator;
import com.chat.entity.User;
import com.chat.entity.dto.CreatorDto;
import com.chat.service.impl.CreatorServiceImpl;
import com.chat.service.impl.UserServiceImpl;
import com.chat.utils.RedisUtil;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ChatCacheInit {


    @Resource
    private UserServiceImpl userService;
    @Resource
    private CreatorServiceImpl creatorService;
    @Resource
    private UserCache userCache;
    @Resource
    private MsgHoderTemp msgHoderTemp;
    @Resource
    private RedisUtil redisUtil;

    @PostConstruct
    public void CacheInit() {

        List<User> list = userService.list();
        list.forEach(u -> userCache.AddOneUser(u.getUserCode(), u));
        Map<String, Object> collect = list.stream()
                .filter(u -> StrUtil.isNotEmpty(u.getAddress()))
                .collect(Collectors.toMap(User::getAddress, Function.identity(), (key1, key2) -> key2));
        redisUtil.hmset("sso:user:address",collect);
        MPJLambdaWrapper<Creator> wrapper = new MPJLambdaWrapper<Creator>()
                .selectAll(Creator.class)
                .select(User::getUserName, User::getPortrait, User::getBio,User::getEmail,User::getAddress)
                .leftJoin(User.class, User::getUserCode, Creator::getUserCode)
                .eq(Creator::getOnline, 1);
        List<CreatorDto> creatorDtos = creatorService.selectJoinList(CreatorDto.class, wrapper);
        for (CreatorDto creatorDto : creatorDtos) {
            userCache.addInternal(creatorDto.getAddress(),""+creatorDto.getUserCode());
           // log.info(creatorDto.getEmail()  );
        }
    }
}
