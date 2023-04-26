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


    @AuthToken(validate = false)
    @PostMapping("/sendMsgv1")
    public R sendMsgv1(@RequestBody InteractiveDto interactiveDto, @RequestHeader(value = "token") String token) throws Exception {


        String curr = null;
        try {
            curr = JwtUtils.getUser(token, SecurityConstant.USER_ID);
            if (curr == null) {
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
//            if (curr.equals(creator)) {
//                JSONArray receive = aptosService.viewReceive(userInfo.getAddress(), creatorUser.getAddress());
//                if(receive.size()>0){
//                    aptosService.sysSendMsg ( creatorUser.getAddress(),userInfo.getAddress());
//                }
//            }
            Interactive interactive = new Interactive();
            Long timestamp = System.currentTimeMillis();
            String transactionOrderId = timestamp + userCode + creator;
            interactive.setId(transactionOrderId);
            interactive.setUserCode(Objects.equals(userCode, curr) ? userCode : creator);
            interactive.setCreator(Objects.equals(userCode, curr) ? creator : userCode);
            interactive.setToken(TokenEnum.APT.getSymbol());
            interactive.setAmount("" + new BigDecimal(ChatConfig.CHAT_FEE).multiply(BigDecimal.TEN.pow(8)).toBigInteger().longValue());
            interactive.setCreateTime(new Date(timestamp).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
            interactive.setUpdateTime(new Date(timestamp).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
            interactive.setStatus(curr.equals(creator)?InteractiveStautsEnum.REPLY.getType():InteractiveStautsEnum.SEND.getType());
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
