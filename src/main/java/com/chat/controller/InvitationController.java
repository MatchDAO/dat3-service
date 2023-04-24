package com.chat.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.chat.common.AuthToken;
import com.chat.common.ConsumerEnum;
import com.chat.common.R;
import com.chat.common.SecurityConstant;
import com.chat.entity.InvitationRewards;
import com.chat.service.impl.InvitationRewardsServiceImpl;
import com.chat.utils.JwtUtils;
import com.chat.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.List;

/**
 * <p>
 * 邀请/奖励相关
 * </p>
 *
 * @author jetBrains
 * @since 2022-12-21
 */
@Slf4j
@RestController
@RequestMapping("/invitation")
public class InvitationController {
    @Resource
    private InvitationRewardsServiceImpl invitationRewardsService;

    @GetMapping("/codeList")
    public R codeList(@RequestHeader(value = "token") String token) {
        String userCode = null;
        try {
            userCode = JwtUtils.getUser(token, SecurityConstant.USER_ID);
            if (userCode == null) {
                return R.success(0);
            }
        } catch (Exception e) {
            log.error("`Invalid user" + e.fillInStackTrace());
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        List arr = invitationRewardsService.invitationCodeList(userCode);
        if (!CollUtil.isEmpty(arr)) {
            return R.success(arr);
        }
        return R.error();
    }

    @GetMapping("/rewards")
    public R rewardsList(@RequestHeader(value = "token") String token) {
        String userCode = null;
        try {
            userCode = JwtUtils.getUser(token, SecurityConstant.USER_ID);
            if (userCode == null) {
                return R.success(0);
            }
        } catch (Exception e) {
            log.error("`Invalid user" + e.fillInStackTrace());
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        List list = invitationRewardsService.rewardsList(userCode);
        return R.success(list);
    }


    @AuthToken(validate = false)
    @GetMapping("/updateRewards")
    public R updateRewards(@RequestParam String type,
                           @RequestParam String userCode,
                           @RequestParam String invited,
                           @RequestParam String amount,
                           @RequestParam String re,
                           @RequestParam String pwd) {

        if (StrUtil.isEmpty(type)
                || StrUtil.isEmpty(userCode)
                || StrUtil.isEmpty(invited)
                || StrUtil.isEmpty(amount)
                || StrUtil.isEmpty(re)
                || StrUtil.isEmpty(pwd)
                || !"defrewgcerfew1123r2rfebfv".equals(pwd)) {
            return R.error();
        }
        InvitationRewards one = invitationRewardsService.getOne(new LambdaQueryWrapper<InvitationRewards>()
                .eq(InvitationRewards::getInvited, invited)
                .eq(InvitationRewards::getUserCode, userCode)
                .eq(InvitationRewards::getType, type));
        if (one != null) {
            amount = new BigInteger(amount).add(new BigInteger(one.getAmount())).toString();
            re = new BigInteger(re).add(new BigInteger(one.getRewards())).toString();
        }
        LambdaUpdateWrapper<InvitationRewards> set = new LambdaUpdateWrapper<InvitationRewards>()
                .eq(InvitationRewards::getInvited, invited)
                .eq(InvitationRewards::getUserCode, userCode)
                .eq(InvitationRewards::getType, type);
        set.set(InvitationRewards::getAmount, amount);
        set.set(InvitationRewards::getRewards, re);
        if (invitationRewardsService.update(null, set)) {
            return R.success();
        }
        return R.error();
    }

    @AuthToken(validate = false)
    @GetMapping("/getRewards")
    public R getRewards(@RequestParam String pwd) {
        if (!"defrewgcerfew1123r2rfebfv".equals(pwd)) {
            return R.error();
        }
        List<InvitationRewards> list = invitationRewardsService.list(new LambdaQueryWrapper<InvitationRewards>()
                .eq(InvitationRewards::getType, ConsumerEnum.SPEND.getSign()));
        return R.success(list);
    }
}
