package com.chat.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.chat.cache.MsgHoderTemp;
import com.chat.common.*;
import com.chat.config.ChatConfig;
import com.chat.entity.Interactive;
import com.chat.entity.User;
import com.chat.entity.dto.InteractiveDto;
import com.chat.entity.dto.UserDto;
import com.chat.service.AptosService;
import com.chat.service.InteractiveAssetActivityService;
import com.chat.service.impl.InteractiveServiceImpl;
import com.chat.service.impl.UserServiceImpl;
import com.chat.utils.JwtUtils;
import com.chat.utils.MessageUtils;
import com.chat.utils.aptos.request.v1.model.Response;
import com.chat.utils.aptos.request.v1.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.*;

/**
 * <p>
 * 用户互动/发消息
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
@Slf4j
@RestController
@RequestMapping("/interactive")
@AuthToken
public class InteractiveController {
    @Resource
    private MsgHoderTemp msgHoderTemp;
    @Resource
    private InteractiveServiceImpl interactiveService;
    @Resource
    InteractiveAssetActivityService interactiveAssetActivityService;
    @Resource
    private AptosService aptosService;
    @Resource
    private UserServiceImpl userService;

    @PostMapping("/sendMsg")
    public R sendMsg(@RequestBody InteractiveDto interactiveDto) {
        R r = interactiveService.sendMsg(interactiveDto);
        return r;
    }

    //0:当前用户或当前会话user信息异常
    //1:u1是u2的消费者
    //2:u2是u1的消费者
    @GetMapping("/before/{u2}")
    public R before(@PathVariable String u2, @RequestHeader(value = "token") String token) {
        String u1 = null;
        try {
            u1 = JwtUtils.getUserKey(token);
            if (u1 == null || u2 == null || u1.equals(u2)) {
                return R.success(0);
            }
        } catch (Exception e) {
            log.error("`Invalid user" + e.fillInStackTrace());
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        R r = interactiveService.before(u1, u2);
        return r;
    }

    @PostMapping("/reply")
    public R reply(@RequestBody InteractiveDto interactiveDto) {
        String userCode = interactiveDto.getUserCode();
        String creator = interactiveDto.getCreator();
        Long timestamp = interactiveDto.getTimestamp();
        R r = interactiveService.reply(creator, userCode, timestamp);
        return r;
    }


    @PostMapping("/replyv1")
    public R replyv1(@RequestBody Map<String, String> request, @RequestHeader(value = "token") String token) throws InterruptedException {
//        public R callRequestV1(@RequestBody Map<String, String> request, @RequestHeader(value = "token") String token) throws Exception {
        String userCode = "";
        try {
            userCode = JwtUtils.getUser(token, SecurityConstant.USER_ID);
            if (StrUtil.isEmpty(userCode)) {
                return R.error(MessageUtils.getLocale("user.invalid"));
            }
        } catch (Exception e) {
            log.error("`Invalid user" + e.fillInStackTrace());
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        log.info(request + "");
        String from = request.getOrDefault("from", "");
        String to = request.getOrDefault("to", "");

        userCode = userCode.equals(request.getOrDefault("userCode", "_")) ? userCode : "";
        if (StrUtil.isEmpty(from) || StrUtil.isEmpty(to) || StrUtil.isEmpty(userCode)) {
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        User fromUser = userService.getById(from);
        User toUser = userService.getById(to);

        Interactive interactive = new Interactive();

        try {
            JSONArray objects = aptosService.view_call_1(fromUser.getAddress(), toUser.getAddress());
            log.info("replyv1 " + objects);
            if (objects != null && objects.size() > 0) {
                Response<Transaction> transactionResponse = aptosService.sys_call_1(fromUser.getAddress(), toUser.getAddress(), "0");
                Long timestamp = System.currentTimeMillis();
                String transactionOrderId = timestamp + userCode + toUser.getUserCode();
                interactive.setId(transactionOrderId);
                interactive.setUserCode(fromUser.getUserCode());
                interactive.setCreator(toUser.getUserCode());
                interactive.setToken(TokenEnum.APT.getSymbol());
                interactive.setAmount(new BigDecimal(ChatConfig.CHAT_FEE).multiply(BigDecimal.TEN.pow(8)).toBigInteger().toString());
                interactive.setCreateTime(new Date(timestamp).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
                interactive.setUpdateTime(new Date(timestamp).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
                interactive.setStatus(InteractiveStautsEnum.REPLY.getType());
                interactive.setTimeMillis(System.currentTimeMillis());
                boolean save = interactiveService.save(interactive);
                log.info("replyv1 " + transactionResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return R.success();
    }


    @AuthToken(validate = false)
    @PostMapping("/sendMsgv1")
    public R sendMsgv1(@RequestBody InteractiveDto interactiveDto, @RequestHeader(value = "token") String token) throws Exception {


        String from = null;
        try {
            from = JwtUtils.getUser(token, SecurityConstant.USER_ID);
            if (from == null) {
                return R.success(0);
            }
        } catch (Exception e) {
            log.error("`Invalid user" + e.fillInStackTrace());
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        String userCode = interactiveDto.getUserCode();
        String creator = interactiveDto.getCreator();


        try {
            UserDto userInfo = userService.getUserInfo(UserDto.builder().userCode(userCode).build(), true);
            if (userInfo == null || StrUtil.isEmpty(userInfo.getAddress())) {
                return R.error(MessageUtils.getLocale("user.invalid"));
            }
            UserDto creatorUser = userService.getUserInfo(UserDto.builder().userCode(creator).build(), true);
            if (creatorUser == null || StrUtil.isEmpty(creatorUser.getAddress())) {
                return R.error(MessageUtils.getLocale("user.invalid"));
            }
            R before = interactiveService.before(from, creator);
            Integer isSender= (Integer) before.getData();
            if (isSender==1 &&from.equals(userCode)) {
                Vector<Long> msgHoder = msgHoderTemp.getMsgHoder(userInfo.getAddress() + "@" + creatorUser.getAddress()+"@0");
                int size = msgHoder.size();

                JSONArray userAssets = aptosService.getUserAssets(userInfo.getAddress());
                Long aLong = userAssets != null ? userAssets.getLong(5, 0L) : 0;
                long fee = aLong - (size + 1) * (new BigDecimal(ChatConfig.CHAT_FEE).multiply(BigDecimal.TEN.pow(8)).toBigInteger().longValue());
                log.info("msgHoder.size() " + size + ", amount:" + aLong + "," + fee);
                if (fee < 0) {
                    JSONObject entries = new JSONObject();
                    entries.set("amount", aLong);
                    entries.set("fee", (size + 1) * (new BigDecimal(ChatConfig.CHAT_FEE).multiply(BigDecimal.TEN.pow(8)).toBigInteger().longValue()));
                    return R.custom(500, MessageUtils.getLocale("result.302"), entries);
                }
            }
            if (isSender==1 &&from.equals(userCode)) {
                if (aptosService.task.size()<1) {
                    aptosService.dat3_routel_send_msg(userInfo.getAddress(), creatorUser.getAddress(), 1,"0");
                }else {
                    msgHoderTemp.addMsgHoder(userInfo.getAddress() + "@" + creatorUser.getAddress()+"@0");
                }

            } else {
                Vector<Long> ifPresent = msgHoderTemp.getIfPresent(userInfo.getAddress() + "@" + creatorUser.getAddress()+"@0");
                int allSize = CollUtil.isEmpty(ifPresent) ? 0 : ifPresent.size();
                JSONArray objects = aptosService.view_call_1(userInfo.getAddress(), creatorUser.getAddress());
                allSize = allSize + (objects == null ? 0 : objects.size());
                if (allSize > 0) {
                    msgHoderTemp.addMsgHoder(creatorUser.getAddress() + "@" + userInfo.getAddress()+"@1");
                }

            }


            Interactive interactive = new Interactive();
            Long timestamp = System.currentTimeMillis();
            String transactionOrderId = timestamp + userCode + creator;
            interactive.setId(transactionOrderId);
            interactive.setUserCode(Objects.equals(userCode, from) ? userCode : creator);
            interactive.setCreator(Objects.equals(userCode, from) ? creator : userCode);
            interactive.setToken(TokenEnum.APT.getSymbol());
            interactive.setAmount("" + new BigDecimal(ChatConfig.CHAT_FEE).multiply(BigDecimal.TEN.pow(8)).toBigInteger().longValue());
            interactive.setCreateTime(new Date(timestamp).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
            interactive.setUpdateTime(new Date(timestamp).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
            interactive.setStatus(InteractiveStautsEnum.SEND.getType());
            interactive.setTimeMillis(System.currentTimeMillis());
            boolean save = interactiveService.save(interactive);
            log.info("interactiveService.save :" + save);
            if (save) {
                return R.success();
            }
            return R.error();
        } catch (Exception e) {
            log.error(" " + e.fillInStackTrace());
            e.printStackTrace();
        }
        return R.error();
    }



}
