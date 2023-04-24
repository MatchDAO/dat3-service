package com.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chat.cache.UserCache;
import com.chat.common.ChatPage;
import com.chat.entity.Creator;
import com.chat.entity.User;
import com.chat.entity.dto.CreatorDto;
import com.chat.entity.dto.CreatorSearchDto;
import com.chat.mapper.CreatorMapper;
import com.chat.service.CreatorService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
@Service
public class CreatorServiceImpl extends MPJBaseServiceImpl<CreatorMapper, Creator> implements CreatorService {

    @Resource
    private UserCache userCache;

    @Override
    public ChatPage<CreatorDto> popular(Page<CreatorDto> page, CreatorSearchDto query) {
        MPJLambdaWrapper<Creator> wrapper = new MPJLambdaWrapper<Creator>()
                .selectAll(Creator.class)
                .select(User::getUserName, User::getPortrait, User::getBio,User::getAddress,User::getGender)
                .leftJoin(User.class, User::getUserCode, Creator::getUserCode);
        if (query != null && !StrUtil.isEmpty(query.getUserCode())) {
            wrapper.eq(User::getUserCode, query.getUserCode());
        }
        //单独查询
        if (query != null && !StrUtil.isEmpty(query.getKeyword())) {
            wrapper = new MPJLambdaWrapper<Creator>()
                    .select(Creator::getProfession,Creator::getProfessionBio,Creator::getOnline,Creator::getProfit7d,Creator::getInteractive7d,Creator::getLastOnlineTime )
                    .select(User::getUserCode,User::getUserName, User::getPortrait, User::getBio,User::getAddress,User::getGender)
                    .rightJoin(User.class, User::getUserCode, Creator::getUserCode);
            String keyword = query.getKeyword();
            keyword=keyword.trim().toLowerCase();
            if(keyword.startsWith("0x")){
                wrapper.like(User::getAddress, keyword);
            }else {
                wrapper.like(User::getUserName, keyword);
            }
            page.setOptimizeCountSql(false);
            IPage<CreatorDto> creatorDtoIPage = selectJoinListPage(page, CreatorDto.class, wrapper);
            if (!CollUtil.isEmpty(creatorDtoIPage.getRecords())) {
                creatorDtoIPage.getRecords().forEach(s -> {
                    if (StrUtil.isEmpty(s.getProfession())) {
                        s.setProfession("[]");
                    }
                    s.setProfessionBio(s.getBio());
                    s.setShow((s.getOnline() != null && s.getOnline() == 1)?true:false);
                    s.setOnline((s.getOnline() != null && s.getOnline() == 1) ? System.currentTimeMillis() : userCache.userOnlineGet(s.getUserCode()));
                });
            }
            ChatPage<CreatorDto> objectChatPage = new ChatPage<>();
            objectChatPage.setCurrent(creatorDtoIPage.getCurrent());
            objectChatPage.setTotal(creatorDtoIPage.getTotal());
            objectChatPage.setSize(creatorDtoIPage.getSize());
            objectChatPage.setRecords(creatorDtoIPage.getRecords());
            return objectChatPage;
        }
        //在线的人
        wrapper.and(and1 -> and1.eq(Creator::getOnline, 1)
                .or()
                .gt(Creator::getLastOnlineTime, LocalDateTime.now().plusSeconds(-60)));

        //根据专业查询并排序
        if (query != null && JSONUtil.isTypeJSONArray(query.getProfession())) {
//            JSON_CONTAINS(JSON_ARRAY( ),keywords ) ORDER BY
            JSONArray jsonArray = JSON.parseArray(query.getProfession());
            StringBuilder lastSql = new StringBuilder();

            lastSql.append(" and JSON_CONTAINS(profession,JSON_ARRAY(");//JSON_CONTAINS(JSON_ARRAY(" "),keywords->'$.keywords')
            jsonArray.forEach(s -> {
                lastSql.append("'").append(s).append("',");
            });
            lastSql.deleteCharAt(lastSql.length() - 1);
            lastSql.append(")) order by profit7d desc ");
            wrapper.last(lastSql.toString());
        } else {
            //添加排序
            wrapper.orderByDesc(Creator::getProfit7d);
        }
        page.setOptimizeCountSql(false);
        IPage<CreatorDto> creatorDtoIPage = selectJoinListPage(page, CreatorDto.class, wrapper);
        if (!CollUtil.isEmpty(creatorDtoIPage.getRecords())) {
            creatorDtoIPage.getRecords().forEach(s -> {
                if (StrUtil.isEmpty(s.getProfession())) {
                    s.setProfession("[]");
                }

                s.setShow((s.getOnline() != null && s.getOnline() == 1)?true:false);
                s.setProfessionBio(s.getBio());
                s.setOnline((s.getOnline() != null && s.getOnline() == 1) ? System.currentTimeMillis() : userCache.userOnlineGet(s.getUserCode()));
            });
        }
        ChatPage<CreatorDto> objectChatPage = new ChatPage<>();
        objectChatPage.setCurrent(creatorDtoIPage.getCurrent());
        objectChatPage.setTotal(creatorDtoIPage.getTotal());
        objectChatPage.setSize(creatorDtoIPage.getSize());
        objectChatPage.setRecords(creatorDtoIPage.getRecords());
        return objectChatPage;
    }
}
