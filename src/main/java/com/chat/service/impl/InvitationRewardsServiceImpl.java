package com.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.chat.common.TokenEnum;
import com.chat.entity.InvitationCodeTotal;
import com.chat.entity.InvitationRewards;
import com.chat.entity.User;
import com.chat.entity.dto.InvitationRewardsDto;
import com.chat.mapper.InvitationRewardsMapper;
import com.chat.service.InvitationRewardsService;
import com.chat.utils.InvitationCodeUtil;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jetBrains
 * @since 2022-12-21
 */
@Service
public class InvitationRewardsServiceImpl extends MPJBaseServiceImpl<InvitationRewardsMapper, InvitationRewards> implements InvitationRewardsService {

    @Resource
    private InvitationCodeTotalServiceImpl invitationCodeTotalService;


    @Override
    public List invitationCodeList(String userCode) {
        InvitationCodeTotal byId = invitationCodeTotalService.getById(userCode);
        if (byId == null) {
            InvitationCodeTotal tempAdd = new InvitationCodeTotal();
            tempAdd.setUserCode(userCode);
            tempAdd.setTotal(2);
            tempAdd.setUsed(0);
            invitationCodeTotalService.save(tempAdd);
        }
        byId = invitationCodeTotalService.getById(userCode);
        Integer total = byId.getTotal();
        Integer used = byId.getUsed();
        List<HashMap<String, Object>> codeList = new ArrayList<>();
//Spend and earn
        for (int i = 1; i <= total; i++) {
            HashMap<String, Object> te = new HashMap<>();
            te.put("ic", InvitationCodeUtil.encode(Long.parseLong("" + userCode + i)));
            te.put("state", 0);
            codeList.add(te);
        }
        List<InvitationRewards> listRewards = this.list(new LambdaQueryWrapper<InvitationRewards>()
                .eq(InvitationRewards::getUserCode, userCode));
        if (!CollUtil.isEmpty(listRewards)) {
            for (InvitationRewards listReward : listRewards) {
                if (listReward.getIndex() <= total) {
                    codeList.get(listReward.getIndex() - 1).put("state", 1);
                }
            }
        }

        return codeList;
    }

    @Override
    public Boolean setInvitationRewards(String type, String userCode, String invitationCode) {
        int index = 0;
        String userCode1 = "";
        try {
            index = Math.toIntExact(InvitationCodeUtil.decodeIndex(invitationCode));
            userCode1 = InvitationCodeUtil.decodeUserCode(invitationCode);
        } catch (Exception e) {
            log.error("error invitationCode");
            return false;
        }

        InvitationRewards invitationRewards = new InvitationRewards();
        invitationRewards.setInvitationCode(invitationCode);
        invitationRewards.setInvited(userCode);
        invitationRewards.setIndex(index);
        invitationRewards.setUserCode(userCode1);
        invitationRewards.setType(type);
        invitationRewards.setRewards("0");
        invitationRewards.setToken(TokenEnum.ETH.getSymbol());
        return save(invitationRewards);
    }

    @Override
    public boolean updateInvitationRewards(String type, String amount, String invited, String invitationCode) {
        InvitationRewards one = getOne(new LambdaQueryWrapper<InvitationRewards>()
                .eq(InvitationRewards::getInvited, invited)
                .eq(InvitationRewards::getType, type)
                .eq(InvitationRewards::getInvitationCode, invitationCode).last("limit 1"));
        if (one != null) {
            LambdaUpdateWrapper<InvitationRewards> set = new LambdaUpdateWrapper<InvitationRewards>()
                    .eq(InvitationRewards::getInvited, invited)
                    .eq(InvitationRewards::getType, type)
                    .eq(InvitationRewards::getInvitationCode, invitationCode);
            set.set(InvitationRewards::getAmount, new BigInteger(one.getAmount()).add(new BigInteger(amount)));
            if (update(null, set)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<InvitationRewardsDto> rewardsList(String userCode) {
        MPJLambdaWrapper<InvitationRewards> wrapper = new MPJLambdaWrapper<InvitationRewards>()
                .selectAll(InvitationRewards.class)
                .select(User::getUserName, User::getPortrait)
                .leftJoin(User.class, User::getUserCode, InvitationRewards::getInvited)
                .eq(InvitationRewards::getUserCode, userCode)
                .orderByDesc(InvitationRewards::getRewards, InvitationRewards::getCreateTime);


        List<InvitationRewardsDto> invitationRewardsDtos = selectJoinList(InvitationRewardsDto.class, wrapper);
        return invitationRewardsDtos;
    }
}
