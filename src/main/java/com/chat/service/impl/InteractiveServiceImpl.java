package com.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.cache.ChatFeeCache;
import com.chat.cache.OrderCache;
import com.chat.common.*;
import com.chat.config.ChatConfig;
import com.chat.config.own.PrivateConfig;
import com.chat.entity.Creator;
import com.chat.entity.Interactive;
import com.chat.entity.User;
import com.chat.entity.dto.InteractiveDto;
import com.chat.entity.dto.UserDto;
import com.chat.entity.dto.WalletUserResult;
import com.chat.mapper.InteractiveMapper;
import com.chat.service.InteractiveService;
import com.chat.service.TransactionUtils;
import com.chat.utils.Coder;
import com.chat.utils.DESCoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
@Service
public class InteractiveServiceImpl extends ServiceImpl<InteractiveMapper, Interactive> implements InteractiveService {
    @Resource
    private UserServiceImpl userService;
    @Resource
    private CreatorServiceImpl creatorService;
    @Resource
    private TransactionUtils transactionUtils;
    @Resource
    private InteractiveAssetActivityServiceImpl interactiveAssetActivityService;
    @Resource
    private  InvitationCodeTotalServiceImpl invitationCodeTotalService;

    @Resource
    private  InvitationRewardsServiceImpl invitationRewardsService;
    @Override
    public R   sendMsg(InteractiveDto interactiveDto) {
        String userCode = interactiveDto.getUserCode();
        String creator = interactiveDto.getCreator();
        if (new BigInteger(interactiveDto.getAmount()).subtract(ChatFeeCache.chatFee).abs().longValue() - ChatFeeCache.chatFeeError > 0) {
            return R.error("ERROR AMOUNT");
        }
        try {
            UserDto userInfo = userService.getUserInfo(UserDto.builder().userCode(userCode).build(),true);
            if (userInfo == null || StrUtil.isEmpty(userInfo.getWallet())) {
                return R.error("no user:"+userCode);
            }
            UserDto creatorUser = userService.getUserInfo(UserDto.builder().userCode(creator).build(),true);
            if (creatorUser == null || StrUtil.isEmpty(creatorUser.getWallet())) {
                return R.error("no user :"+creator);
            }
//            Creator byId1 = creatorService.getById(creator);
//            if (byId1 == null ) {
//                return R.error("no Creator :"+ creator);
//            }
            Interactive interactive = new Interactive();
            Long timestamp = interactiveDto.getTimestamp();
            String transactionOrderId = timestamp + userCode + creator;
            interactive.setId(timestamp + userCode + creator);
            interactive.setUserCode(userCode);
            interactive.setCreator(creator);
            interactive.setToken(TokenEnum.ETH.getSymbol());
            interactive.setAmount(interactiveDto.getAmount());
            interactive.setCreateTime(new Date(timestamp).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
            interactive.setUpdateTime(new Date(timestamp).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
            interactive.setStatus(InteractiveStautsEnum.SEND.getType());
            Interactive byId = getById(interactive.getId());
            if (byId != null) {
                return R.error("Sending too often, please send later");
            }
            String password = URLEncoder.encode(Coder.encryptBASE64(DESCoder.encrypt(userInfo.getWallet().getBytes(), PrivateConfig.TR_KEY))
                    .replace("\n", ""));
            WalletUserResult walletUserResult = transactionUtils.sendMsg(userInfo.getWallet(), creatorUser.getWallet(), TokenEnum.ETH.getToken(), interactiveDto.getAmount(), password, transactionOrderId);
            if (walletUserResult != null) {
                if (walletUserResult.getCode() == ResultCode.FROZEN.getCode()) {
                    return R.custom(walletUserResult.getCode(), "Insufficient amount");
                }
                if (walletUserResult.getCode() == ResultCode.FAILED.getCode()) {
                    return R.custom(walletUserResult.getCode(), walletUserResult.getMessage());
                }
                if (walletUserResult.getCode() == ResultCode.BAD_REQUEST.getCode()) {
                    return R.custom(500, "");
                }
                if (walletUserResult.getCode() == ResultCode.SUCCESS.getCode()) {
                    boolean save = save(interactive);


//奖励邀请码
                    invitationCodeTotalService.changeTotal(ActionEnum.SEND_MSG.getSign(), 1,userCode,0);
                    return R.success(save);
                }

            }
        } catch (Exception e) {
            log.error("" + e);
        }

        return R.error();
    }

    @Override
    public R sendMsgv1(InteractiveDto interactiveDto) {
        return null;
    }
    @Override
    public R reply(String creator, String userCode, Long timestamp) {

        //获取所有未回复的消息
        List<Interactive> interactions = list(new LambdaQueryWrapper<Interactive>()
                .eq(Interactive::getUserCode, userCode)
                .eq(Interactive::getCreator, creator)
                .eq(Interactive::getStatus, InteractiveStautsEnum.SEND.getType())
                .gt(Interactive::getCreateTime, LocalDateTime.now().plusHours(-12)));
        if (CollUtil.isEmpty(interactions)) {
            return R.success("No unreplied  messages");
        }
        UserDto creatorUser = userService.getUserInfo(UserDto.builder().userCode(creator).build(),true);
        UserDto consumer = userService.getUserInfo(UserDto.builder().userCode(userCode).build(),true);
        if (creatorUser == null
                || StrUtil.isEmpty(creatorUser.getWallet())
                || consumer == null
                || StrUtil.isEmpty(consumer.getWallet())) {
            return R.error("no user");
        }

        //todo 暂有eth
        int sentCount = interactions.size();
        String transactionOrderId = "" + timestamp + creator + consumer.getUserCode();
        LongSummaryStatistics statistics = interactions.stream()
                .filter(s -> !StrUtil.isEmpty(s.getAmount()))
                .collect(Collectors.summarizingLong(s -> new BigInteger(s.getAmount()).longValue()));
        long sumAmount = statistics.getSum();
        try {
            String password = URLEncoder.encode(Coder.encryptBASE64(DESCoder.encrypt(creatorUser.getWallet().getBytes(), PrivateConfig.TR_KEY))
                    .replace("\n", ""));
            WalletUserResult reply = transactionUtils.reply(creatorUser.getWallet()
                    , consumer.getWallet()
                    , TokenEnum.ETH.getToken()
                    , Long.toString(sumAmount)
                    , sentCount
                    , password, transactionOrderId);
            if (reply.getCode() == ResultCode.SUCCESS.getCode()) {
                interactiveAssetActivityService.addAssetActivity(creatorUser.getWallet()
                        , consumer.getWallet(), TokenEnum.ETH.getToken(), Long.toString(sumAmount), sentCount, transactionOrderId);
                List<String> collect = interactions.stream().map(Interactive::getId).collect(Collectors.toList());
                Boolean aBoolean = updateInteractiveStauts(InteractiveStautsEnum.REPLY.getType(), collect);

                if (!StrUtil.isEmpty(consumer.getInvitationCode())) {
                    invitationRewardsService.updateInvitationRewards(ConsumerEnum.SPEND.getSign(),""+sumAmount,consumer.getUserCode(),consumer.getInvitationCode());
                }
                if (!StrUtil.isEmpty(creatorUser.getInvitationCode())) {
                    invitationRewardsService.updateInvitationRewards(ConsumerEnum.EARN.getSign(),new BigDecimal(""+sumAmount).multiply(new BigDecimal("0.02")).toBigInteger().toString(),creatorUser.getUserCode(),creatorUser.getInvitationCode());
                }

                invitationCodeTotalService.changeTotal(ActionEnum.SEND_MSG.getSign(), 1,creator,0);
                return R.success();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.error();
    }

    public synchronized boolean updateInteractiveStauts(String stauts, List<String> interactiveId) {

        InteractiveStautsEnum stautsEnum = InteractiveStautsEnum.valueOf(stauts);
        List<Interactive> list = new ArrayList<>();
        for (int i = 0; i < interactiveId.size(); i++) {
            if (!OrderCache.moreThanOnce(interactiveId.get(i), 1)) {
                list.add(Interactive.builder().id(interactiveId.get(i)).updateTime(new Date(System.currentTimeMillis()).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime()).status(stautsEnum.getType()).build());
            }

        }
        return this.updateBatchById(list);
    }

    @Override
    public R before(String u1, String u2) {
        //当前用户是u1
        Interactive one = getOne(new LambdaQueryWrapper<Interactive>()
                .eq(Interactive::getUserCode, u1)
                .eq(Interactive::getStatus,InteractiveStautsEnum.SEND.getType())
                .eq(Interactive::getCreator, u2).last("limit 1"));
        //u1是u2的消费者
        if (one != null) {
            return R.success(1);
        }
        one = getOne(new LambdaQueryWrapper<Interactive>()
                .eq(Interactive::getUserCode, u2)
                .eq(Interactive::getStatus,InteractiveStautsEnum.SEND.getType())
                .eq(Interactive::getCreator, u1).last("limit 1"));
        //u2是u1的消费者
        if (one != null) {
            return R.success(2);
        }
        //无u1u2的会话记录 则为第一次且u1为u2的消费者
        User byId = userService.getById(u2);
        if (byId != null) {
            return R.success(1);
        }
        return R.success(0);
    }


}
