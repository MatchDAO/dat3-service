package com.chat.controller;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chat.cache.RateLimiterCache;
import com.chat.cache.RtcSessionCache;
import com.chat.cache.UserCache;
import com.chat.common.*;
import com.chat.config.own.PrivateConfig;
import com.chat.entity.Interactive;
import com.chat.entity.PriceRange;
import com.chat.entity.User;
import com.chat.entity.dto.ChannelDto;
import com.chat.entity.dto.PriceGradeUserDto;
import com.chat.entity.dto.WalletBalance;
import com.chat.entity.dto.WalletUserResult;
import com.chat.service.AgoraService;
import com.chat.service.AptosService;
import com.chat.service.TransactionUtils;
import com.chat.service.impl.InteractiveServiceImpl;
import com.chat.service.impl.PriceGradeUserServiceImpl;
import com.chat.service.impl.PriceRangeServiceImpl;
import com.chat.service.impl.UserServiceImpl;
import com.chat.task.queue.AdvanceDelayTask;
import com.chat.task.queue.CallDelayTask;
import com.chat.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.chat.cache.RtcSessionCache.getChannelName;

@RestController
@RequestMapping("/rtc")
@Slf4j
public class RtcController {
    @Resource
    private InteractiveServiceImpl interactiveService;
    @Resource
    private UserServiceImpl userService;
    @Resource
    private UserCache userCache;
    @Resource
    private TransactionUtils transactionUtils;
    @Resource
    private AgoraService agoraService;
    @Resource
    private RtcSessionCache rtcSessionCache;
    @Resource
    private PriceGradeUserServiceImpl priceGradeUserService;
    @Resource
    private PriceRangeServiceImpl priceRangeService;
    public static TimedCache<String, String> times1 = CacheUtil.newTimedCache(60 * 60 * 24 * 1000);
    @Resource
    private DelayQueue<AdvanceDelayTask> myDelayQueue;
    @Resource
    private DelayQueue<CallDelayTask> myCallDelayQueue;

    @Resource
    private AptosService aptosService;

    /**
     * 查看用户定价
     */
    @GetMapping("/grade/{userCode}")
    public R grade(@PathVariable String userCode) throws Exception {
        PriceGradeUserDto grade = priceGradeUserService.grade(userCode);
        return R.success(grade);
    }

    /**
     * 修改用户定价
     */
    @GetMapping("/newGrade/{id}")
    public R grade(@PathVariable Integer id, @RequestHeader(value = "token") String token) {

        String userCode = "";
        try {
            userCode = JwtUtils.getUser(token, SecurityConstant.USER_ID);
            if (StrUtil.isEmpty(userCode)) {
                return R.error("no user info");
            }
        } catch (Exception e)  {
            log.error("`Invalid user" + e.fillInStackTrace());
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        PriceRange byId = priceRangeService.getById(id);
        if (byId == null) {
            id = 1;
        }
        log.info("newGrade,{},{}", userCode, id);
        return R.success(priceGradeUserService.modifyGrade(userCode, id));
    }

    /**
     * 发起视频通话 获取当前用户的临时token,和channelID
     */
    @PostMapping("/channel/callRequest")
    public R callRequest(@RequestBody Map<String, String> request, @RequestHeader(value = "token") String token) throws Exception {
        String userCode = "";
        try {
            userCode = JwtUtils.getUser(token, SecurityConstant.USER_ID);
            if (StrUtil.isEmpty(userCode)) {
                return R.error("no user info");
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
            return R.error();
        }
        if (RateLimiterCache.moreThanOnce("cr" + from + to, 3L)) {
            return R.error("call too frequent");
        }
        //获取to当前单价
        PriceGradeUserDto grade = priceGradeUserService.grade(to);
        User fromUser = userService.getById(from);
        List<WalletBalance> walletBalances = transactionUtils.balance(fromUser.getWallet());
        AtomicBoolean low = new AtomicBoolean(false);

        walletBalances.stream().filter(s -> TokenEnum.ETH.getSymbol().equals(s.getToken())).forEach(a -> {

            if (new BigDecimal(a.getAmount()).subtract(new BigDecimal(grade.getEPrice())).signum() < 0) {
                log.info("{}   {}   {}", a.getAmount(), grade.getEPrice(), new BigDecimal(a.getAmount()).subtract(new BigDecimal(grade.getEPrice())).signum());
                low.set(true);
            }
        });
        //资金不足
        if (low.get()) {
            return R.custom(429, "Insufficient funds");
        }
        List<Interactive> list = interactiveService.list(new LambdaQueryWrapper<Interactive>()
                .eq(Interactive::getCreator, to)
                .ge(Interactive::getCreateTime, new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 1)))
                .eq(Interactive::getStatus, InteractiveStautsEnum.CALL_BEGIN));
        List<Interactive> notEnd = list.stream().filter(s -> !"0000".equals(s.getReserved())).collect(Collectors.toList());
        if (!CollUtil.isEmpty(notEnd)) {
            return R.custom(500, "The other party is talking to others");
        }
        //如果频道不一样
        ChannelDto oldChannel = rtcSessionCache.channel(from, to);
        //初始化通道状态 保存通道id
        ChannelDto channelDto = rtcSessionCache.newChannel(from, to, null, System.currentTimeMillis());

        if (channelDto != null && channelDto.getChannel() != null) {
            //初始化 强制清空频道,踢出所有人
            if (oldChannel != null
                    && StrUtil.isNotEmpty(oldChannel.getChannel())
                    && !channelDto.getChannel().equals(oldChannel.getChannel())) {
                agoraService.suspendChannel(oldChannel.getChannel(), 0);
            }
            agoraService.suspendChannel(channelDto.getChannel(), 0);
            //返回通道token
            String s = agoraService.rtcTokenBuilder(Integer.parseInt(userCode), channelDto.getChannel(), 1800 * 3);
            HashMap<Object, Object> map = new HashMap<>();
            map.put("uid", userCode);
            map.put("channelName", channelDto.getChannel());
            map.put("privilegeExpirationInSeconds", 1800 * 3);
            map.put("token", s);
            return R.success(map);
        }
        return R.error("init channel failed");
    }


    @PostMapping("/channel/callAccept")
    public R callAccept(@RequestBody Map<String, String> request, @RequestHeader(value = "token") String token) throws Exception {
        String userCode = "";
        try {
            userCode = JwtUtils.getUser(token, SecurityConstant.USER_ID);
            if (StrUtil.isEmpty(userCode)) {
                return R.error("no userCode info");
            }
        } catch (Exception e) {
            log.error("`Invalid user" + e.fillInStackTrace());
            return R.error(MessageUtils.getLocale("user.invalid"));
        }

        String from = request.getOrDefault("from", "");
        String to = request.getOrDefault("to", "");
        String accept = request.getOrDefault("accept", "0");
        userCode = userCode.equals(request.getOrDefault("userCode", "_")) ? userCode : "";
        if (StrUtil.isEmpty(from) || StrUtil.isEmpty(to) || StrUtil.isEmpty(userCode)) {
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        if ("1".equals(accept)) {
            if (RateLimiterCache.moreThanOnce("cr" + from + to, 1L)) {
                return R.error("callAccept too frequent");
            }
            //防抖 todo
            User fromUser = userService.getById(from);
            User toUser = userService.getById(to);
            if (fromUser == null || toUser == null) {
                return R.error("no user info");
            }
            //获取to当前单价
            PriceGradeUserDto grade = priceGradeUserService.grade(userCode);

            String channelName = null;
            Long begin = System.currentTimeMillis() + 2000L;
            //初始化通道状态
            ChannelDto channelDto = rtcSessionCache.newChannel(from, to, grade.getEPrice(), begin);
            if (channelDto == null) {
                return R.error("init channel failed");
            }
            channelName = channelDto.getChannel();
            //初始化通道
//            agoraService.suspendChannel(channelName, 0);
            Interactive interactive = new Interactive();
            long timestamp = System.currentTimeMillis();
            try {
                interactive.setId("" + channelDto.getBegin() + channelName);
                interactive.setUserCode(from);
                interactive.setCreator(to);
                interactive.setToken(TokenEnum.ETH.getSymbol());
                interactive.setAmount(grade.getEPrice());
                interactive.setCreateTime(new Date(begin).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
                interactive.setUpdateTime(new Date(begin).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
                interactive.setStatus(InteractiveStautsEnum.CALL_BEGIN.getType());
                interactive.setTimeMillis(begin);
                interactiveService.save(interactive);
            } catch (Exception e) {
                log.error(e + "");
                return R.error("callAccept too frequent");
            }

            //预扣费
            String password = URLEncoder.encode(Coder.encryptBASE64(DESCoder.encrypt(fromUser.getWallet().getBytes(), PrivateConfig.TR_KEY))
                    .replace("\n", ""));
            log.info("transactionUtils.rtcFrozen : {},{},{},{},{} ", fromUser.getWallet(), toUser.getWallet(), grade.getEPrice(), password, begin + "");
            WalletUserResult walletUserResult = transactionUtils.rtcFrozen(fromUser.getWallet(), toUser.getWallet(), grade.getEPrice(), password, begin + "");
            if (walletUserResult.getCode() == 200) {
                //返回用户通道token
                String s = agoraService.rtcTokenBuilder(Integer.parseInt(userCode), channelName, 1800 * 3);
                HashMap<Object, Object> map = new HashMap<>();
                map.put("uid", userCode);
                map.put("channelName", channelName);
                map.put("privilegeExpirationInSeconds", 1800 * 3);
                map.put("token", s);
                // todo 延时器预扣费
                myDelayQueue.add(new AdvanceDelayTask(from, to, channelName, 62 * 1000, 0, channelDto.getBegin()));

                return R.success(map);
            } else {
                return R.error("Internal sever error, please contact support, we are sorry about it");
            }
        } else {
            rtcSessionCache.channelEnd(from, to, 5);
            agoraService.suspendChannel(getChannelName(from, to), 0);
        }
        return R.success();
    }


    @PostMapping("/channel/callEnd")
    public R callEnd(@RequestBody Map<String, String> request, @RequestHeader(value = "token") String token) throws Exception {
        String userCode = "";
        try {
            userCode = JwtUtils.getUser(token, SecurityConstant.USER_ID);
            if (StrUtil.isEmpty(userCode)) {
                return R.error("no user info");
            }
            log.info("userCode :,{}", userCode);
        } catch (Exception e) {
            log.error("Illegal user" + e.fillInStackTrace());
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        log.info(request + "");
        String from = request.getOrDefault("from", "");
        String to = request.getOrDefault("to", "");
        userCode = userCode.equals(request.getOrDefault("userCode", "_")) ? userCode : "";
        log.info("userCode :,{}", userCode);
        if (StrUtil.isEmpty(from) || StrUtil.isEmpty(to) || StrUtil.isEmpty(userCode)) {
            return R.error();
        }
        //获取通道状态
        ChannelDto channelInfo = rtcSessionCache.channel(from, to);
        String frozen = channelInfo.getFrozen();
        if (StrUtil.isEmpty(frozen)) {
            return R.success("frozen");
        }


        //防抖 todo
        User fromUser = userService.getById(from);
        User toUser = userService.getById(to);
        if (fromUser == null || toUser == null) {
            return R.error("no user info");
        }

        log.info("/channel/callEnd " + channelInfo);
        if (channelInfo == null) {
            return R.error(" `Invalid user channel");
        }
        //初始化通道,踢出所有人
        agoraService.suspendChannel(channelInfo.getChannel(), 0);

        //更新通道状态为关闭状态
        Boolean closed = rtcSessionCache.channelEnd(from, to, userCode.equals(from) ? 1 : 2);

        if (!closed) {
            rtcSessionCache.channelEnd(from, to, userCode.equals(from) ? 1 : 2);
            // todo 延时器预扣费
        }

        myDelayQueue.add(new AdvanceDelayTask(channelInfo.getFrom(), channelInfo.getTo(), channelInfo.getChannel(), 500, userCode.equals(from) ? 1 : 2, channelInfo.getBegin()));

        if (RateLimiterCache.moreThanOnce("ce" + from + to, 3L)) {
            return R.success("Hang up operation is too frequent");
        }
        //获取通道id/互动id
        String channelName = channelInfo.getChannel();
        //获取互动开始时间
        Interactive byId = interactiveService.getById(channelInfo.getBegin() + channelName);
        if (byId == null) {
            return R.error("no begin info");
        }
        Long bTime = byId.getTimeMillis();
        log.info("bTime :{},{},{}", bTime, fromUser.getWallet(), toUser.getWallet());

        //预扣费结算
        String password = URLEncoder.encode(Coder.encryptBASE64(DESCoder.encrypt(fromUser.getWallet().getBytes(), PrivateConfig.TR_KEY))
                .replace("\n", ""));
        WalletUserResult walletUserResult = transactionUtils.rtcPayment(fromUser.getWallet(), toUser.getWallet(), password, bTime + "", channelName);
// todo 优化返回值
        if (walletUserResult != null) {
            Long sta = System.currentTimeMillis();
            Interactive interactive = new Interactive();
            interactive.setId(channelInfo.getBegin() + channelName + "0000");
            interactive.setUserCode(from);
            interactive.setCreator(to);
            interactive.setToken(TokenEnum.ETH.getSymbol());
            interactive.setAmount(walletUserResult.getMessage());
            interactive.setCreateTime(new Date(sta).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
            interactive.setUpdateTime(new Date(sta).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
            interactive.setStatus(InteractiveStautsEnum.CALL_END.getType());
            interactive.setTimeMillis(System.currentTimeMillis());

            if (walletUserResult.getCode() == 200) {
                //记录开始和结束
                try {
                    interactiveService.save(interactive);
                } catch (Exception e) {
                    log.error("" + e.fillInStackTrace());
                }


                Interactive updateBegin = new Interactive();
                updateBegin.setId("" + channelInfo.getBegin() + channelName);
                updateBegin.setReserved("0000");
                updateBegin.setUpdateTime(LocalDateTime.now());
                interactiveService.updateById(updateBegin);
                return R.success();

            } else if (walletUserResult.getCode() == 1001) {
                //The payment has been settled.
                Interactive byId1 = interactiveService.getById(interactive.getId());
                if (byId1 == null) {
                    interactiveService.save(interactive);
                    Interactive updateBegin = new Interactive();
                    updateBegin.setId("" + channelInfo.getBegin() + channelName);
                    updateBegin.setReserved("0000");
                    updateBegin.setUpdateTime(LocalDateTime.now());
                    interactiveService.updateById(updateBegin);
                }
                return R.success("1001Internal sever error, please contact support, we are sorry about it");
            } else if (walletUserResult.getCode() == 1002) {
                //no List<AssetActivity>
                Interactive byId1 = interactiveService.getById(interactive.getId());
                if (byId1 == null) {
                    interactiveService.save(interactive);
                    Interactive updateBegin = new Interactive();
                    updateBegin.setId("" + channelInfo.getBegin() + channelName);
                    updateBegin.setReserved("0000");
                    updateBegin.setUpdateTime(LocalDateTime.now());
                    interactiveService.updateById(updateBegin);
                }
                return R.success("1002Internal sever error, please contact support, we are sorry about it");
            }
        }

        return R.error();

    }


    @PostMapping("/channel/state")
    public R channelReward(@RequestBody Map<String, String> request) {
        log.info(request + "");
        String from = request.getOrDefault("from", "");
        String to = request.getOrDefault("to", "");
        String userCode = request.getOrDefault("userCode", "");
        if (StrUtil.isEmpty(from) || StrUtil.isEmpty(to) || StrUtil.isEmpty(userCode)) {
            return R.error();
        }
        ChannelDto channel = rtcSessionCache.channel(from, to);
        if (channel==null) {
            return R.error("not found");
        }
        JSONObject state = JSONUtil.parseObj(channel);
        return R.success(state);
    }


    private static Map<String, User> re = new HashMap<>();
    public static TimedCache<String, String> times1111 = CacheUtil.newTimedCache(180 * 1000);

    @GetMapping("/channel/test/{id}")
    @AuthToken
    public R channelName(@PathVariable String id) throws Exception {
        if ("jncweifhw1".equals(id)) {
            HashMap<Object, Object> map = new HashMap<>();
            if (CollUtil.isEmpty(re)) {
                List<User> list = userService.list();
                re = list.stream().collect(Collectors.toMap(User::getUserCode, Function.identity(), (key1, key2) -> key2));
            }
            Iterator<Map.Entry<String, User>> iterator = re.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, User> entry = iterator.next();
                User value = entry.getValue();
                AtomicReference<String> aLong = new AtomicReference<>(times1111.get(value.getUserCode()));
                if (aLong.get() == null) {
                    List<WalletBalance> walletBalances = transactionUtils.balance(value.getWallet());

                    walletBalances.stream().filter(s -> TokenEnum.ETH.getSymbol().equals(s.getToken())).forEach(a -> {
                        if (!"0".equals(a.getAmount())) {
                            aLong.set(a.getAmount() + ";" + value.getUserCode() + ";" + value.getEmail() + ";" + value.getUserName() + ";");
                            times1111.put(value.getUserCode(), aLong.get());
                            map.put(value.getUserCode(), aLong.get());
                        } else {
                            iterator.remove();
                        }
                    });
                } else {
                    map.put(value.getUserCode(), aLong.get());
                }
            }
            return R.success(map);
        }
        return R.success();
    }

    @PostMapping("/channel/callRequestV1")
    public R callRequestV1(@RequestBody Map<String, String> request, @RequestHeader(value = "token") String token) throws Exception {
        String userCode = "";
        try {
            userCode = JwtUtils.getUser(token, SecurityConstant.USER_ID);
            if (StrUtil.isEmpty(userCode)) {
                return R.error("no user info");
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
            return R.error();
        }
        if (RateLimiterCache.moreThanOnce("cr" + from + to, 2L)) {
            return R.error(MessageUtils.getLocale("call.too_frequent") );
        }
        List<Interactive> list = interactiveService.list(new LambdaQueryWrapper<Interactive>()
                .eq(Interactive::getCreator, to)
                .ge(Interactive::getCreateTime, new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 1)))
                .eq(Interactive::getStatus, InteractiveStautsEnum.CALL_BEGIN));
        List<Interactive> notEnd = list.stream().filter(s -> !"0000".equals(s.getReserved())).collect(Collectors.toList());
        if (!CollUtil.isEmpty(notEnd)) {
            return R.custom(500, MessageUtils.getLocale("call.busy") );
        }
        User fromUser = userCache.getUser(userCode);
        User toUser = userCache.getUser(to);
//        PriceGradeUserDto grade = priceGradeUserService.grade(to);
        log.info("" + fromUser);
        JSONArray feeWith = aptosService.feeWith(toUser.getAddress(), fromUser.getAddress());
        log.info(feeWith.toString());
        if (feeWith != null && feeWith.size() > 0) {
            if (feeWith.getLong(3) <= feeWith.getLong(2)) {
                return R.error(MessageUtils.getLocale("result.302"));
            }
        }
        //如果频道不一样
        ChannelDto oldChannel = rtcSessionCache.channel(from, to);
        //初始化通道状态 保存通道id
        ChannelDto channelDto = rtcSessionCache.newChannel(from, to, null, System.currentTimeMillis());
        if (channelDto != null && channelDto.getChannel() != null) {
            //初始化 强制清空频道,踢出所有人
            if (oldChannel != null
                    && StrUtil.isNotEmpty(oldChannel.getChannel())
                    && !channelDto.getChannel().equals(oldChannel.getChannel())) {
                agoraService.suspendChannel(oldChannel.getChannel(), 0);
            }
            agoraService.suspendChannel(channelDto.getChannel(), 0);
            //返回通道token
            String s = agoraService.rtcTokenBuilder(Integer.parseInt(userCode), channelDto.getChannel(), 1800 * 3);
            HashMap<Object, Object> map = new HashMap<>();
            map.put("uid", userCode);
            map.put("channelName", channelDto.getChannel());
            map.put("privilegeExpirationInSeconds", 1800 * 3);
            map.put("token", s);
            map.put("grade", priceGradeUserService.grade(to));
            return R.success(map);
        }
        return R.error(MessageUtils.getLocale("unknown.error") );
    }



    @PostMapping("/channel/callAcceptV1")
    public R callAcceptV1(@RequestBody Map<String, String> request, @RequestHeader(value = "token") String token) throws Exception {
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

        String from = request.getOrDefault("from", "");
        String to = request.getOrDefault("to", "");
        String accept = request.getOrDefault("accept", "0");
        userCode = userCode.equals(request.getOrDefault("userCode", "_")) ? userCode : "";
        if (StrUtil.isEmpty(from) || StrUtil.isEmpty(to) || StrUtil.isEmpty(userCode)) {
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        if ("1".equals(accept)) {
            if (RateLimiterCache.moreThanOnce("cr" + from + to, 1L)) {
                return R.error(MessageUtils.getLocale("call.too_frequent"));
            }
            //防抖 todo
            User fromUser = userService.getById(from);
            User toUser = userService.getById(to);
            if (fromUser == null || toUser == null) {
                return R.error(MessageUtils.getLocale("user.invalid"));
            }
            //获取to当前单价
            PriceGradeUserDto grade = priceGradeUserService.grade(to);

            String channelName = null;
            Long begin = System.currentTimeMillis() + 2000L;
            //初始化通道状态
            ChannelDto channelDto = rtcSessionCache.newChannel(from, to, grade.getEPrice(), begin);
            if (channelDto == null) {
                return R.error(MessageUtils.getLocale("unknown.error"));
            }
            channelName = channelDto.getChannel();
            //初始化通道
//            agoraService.suspendChannel(channelName, 0);
            //获取to当前单价
            try {
                Interactive interactive = new Interactive();
                long timestamp = System.currentTimeMillis();
                interactive.setId("" + channelDto.getBegin() + channelDto.getChannel()+"1111");
                interactive.setUserCode(from);
                interactive.setCreator(to);
                interactive.setToken(TokenEnum.ETH.getSymbol());
                interactive.setAmount(grade.getEPrice());
                interactive.setCreateTime(new Date(timestamp).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
                interactive.setUpdateTime(new Date(timestamp).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
                interactive.setStatus(InteractiveStautsEnum.CALL_ACCEPT.getType());
                interactive.setTimeMillis(timestamp);
                interactiveService.save(interactive);
            } catch (Exception e) {
                log.error(e + "");
                return R.error(MessageUtils.getLocale("call.too_frequent"));
            }

            //返回用户通道token
            String s = agoraService.rtcTokenBuilder(Integer.parseInt(userCode), channelName, 1800 * 3);
            HashMap<Object, Object> map = new HashMap<>();
            map.put("uid", userCode);
            map.put("channelName", channelName);
            map.put("privilegeExpirationInSeconds", 1800 * 3);
            map.put("token", s);
            // todo 延时器预扣费
            myCallDelayQueue.add(new CallDelayTask(from, to, channelName, 20 * 1000, 99, channelDto.getBegin()));
            //定时查询用户金额是否足够
            myCallDelayQueue.add(new CallDelayTask(from, to, channelName, 50 * 1000, 59, channelDto.getBegin()));

            return R.success(map);
        } else {
            rtcSessionCache.channelEnd(from, to, 5);
            agoraService.suspendChannel(getChannelName(from, to), 0);
        }
        return R.success();
    }
    @PostMapping("/channel/callBegin")
    public R callcallbegin(@RequestBody Map<String, String> request, @RequestHeader(value = "token") String token) throws Exception {
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

        String from = request.getOrDefault("from", "");
        String to = request.getOrDefault("to", "");
        userCode = userCode.equals(request.getOrDefault("userCode", "_")) ? userCode : "";
        if (StrUtil.isEmpty(from) || StrUtil.isEmpty(to) || StrUtil.isEmpty(userCode)) {
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        if (RateLimiterCache.moreThanOnce("crbg" + from + to, 1L)) {
            return R.error(MessageUtils.getLocale("call.too_frequent"));
        }
        //获取通道状态
        ChannelDto channelInfo = rtcSessionCache.channel(from, to);

        if (channelInfo==null) {
            return R.error();
        }
        ChannelDto channelDto=  rtcSessionCache.newChannelBegin(from,to, System.currentTimeMillis());
        //获取to当前单价
        PriceGradeUserDto grade = priceGradeUserService.grade(to);
        try {
            Interactive interactive = new Interactive();
            long timestamp = System.currentTimeMillis();
            interactive.setId("" + channelDto.getBegin() + channelDto.getChannel());
            interactive.setUserCode(from);
            interactive.setCreator(to);
            interactive.setToken(TokenEnum.ETH.getSymbol());
            interactive.setAmount(grade.getEPrice());
            interactive.setCreateTime(new Date(timestamp).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
            interactive.setUpdateTime(new Date(timestamp).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
            interactive.setStatus(InteractiveStautsEnum.CALL_BEGIN.getType());
            interactive.setTimeMillis(timestamp);
            interactiveService.save(interactive);
        } catch (Exception e) {
            log.error(e + "");
            return R.error(MessageUtils.getLocale("call.too_frequent"));
        }
        myCallDelayQueue.add(new CallDelayTask(from, to, channelInfo.getChannel(), 6 * 1000, 0,channelDto.getBegin()));
        return R.success();
    }
    @PostMapping("/channel/oneMinute")
    public R callOneMinute(@RequestBody Map<String, String> request, @RequestHeader(value = "token") String token) throws Exception {
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

        String from = request.getOrDefault("from", "");
        String to = request.getOrDefault("to", "");
        userCode = userCode.equals(request.getOrDefault("userCode", "_")) ? userCode : "";
        if (StrUtil.isEmpty(from) || StrUtil.isEmpty(to) || StrUtil.isEmpty(userCode)) {
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        if (RateLimiterCache.moreThanOnce("crbg" + from + to, 1L)) {
            return R.error(MessageUtils.getLocale("call.too_frequent"));
        }
        //获取通道状态
        ChannelDto channelInfo = rtcSessionCache.channel(from, to);

        if (channelInfo==null) {
            return R.error();
        }
        Integer frozenTimes = channelInfo.getFrozenTimes();
        if (rtcSessionCache.channelFrozen(from, to, "")) {
            frozenTimes += 1;
        }
        ;
        //获取to当前单价
        PriceGradeUserDto grade = priceGradeUserService.grade(to);
        try {
            Interactive interactive = new Interactive();
            long timestamp = System.currentTimeMillis();
            interactive.setId("" + channelInfo.getBegin() + channelInfo.getChannel() + 000 + frozenTimes);
            interactive.setUserCode(from);
            interactive.setCreator(to);
            interactive.setToken(TokenEnum.ETH.getSymbol());
            interactive.setAmount(grade.getEPrice());
            interactive.setCreateTime(new Date(timestamp).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
            interactive.setUpdateTime(new Date(timestamp).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
            interactive.setStatus(InteractiveStautsEnum.CALL_ONCE.getType());
            interactive.setTimeMillis(timestamp);
            interactiveService.save(interactive);
        } catch (Exception e) {
            log.error(e + "");
            return R.error(MessageUtils.getLocale("call.too_frequent"));
        }
        myCallDelayQueue.add(new CallDelayTask(from, to, channelInfo.getChannel(), 5 * 1000, 100, channelInfo.getBegin()));
        return R.success();
    }
    @PostMapping("/channel/callEndV1")
    public R callEndV1(@RequestBody Map<String, String> request, @RequestHeader(value = "token") String token) throws Exception {
        String userCode = "";
        try {
            userCode = JwtUtils.getUser(token, SecurityConstant.USER_ID);
            if (StrUtil.isEmpty(userCode)) {
                return R.error(MessageUtils.getLocale("user.invalid"));
            }
            log.info("userCode :,{}", userCode);
        } catch (Exception e) {
            log.error("`Invalid user" + e.fillInStackTrace());
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        log.info(request + "");
        String from = request.getOrDefault("from", "");
        String to = request.getOrDefault("to", "");
        userCode = userCode.equals(request.getOrDefault("userCode", "_")) ? userCode : "";
        log.info("userCode :,{}", userCode);
        if (StrUtil.isEmpty(from) || StrUtil.isEmpty(to) || StrUtil.isEmpty(userCode)) {
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        //获取通道状态
        ChannelDto channelInfo = rtcSessionCache.channel(from, to);
        String frozen = channelInfo.getFrozen();



        //防抖 todo
        User fromUser = userService.getById(from);
        User toUser = userService.getById(to);
        if (fromUser == null || toUser == null) {
            return R.error(MessageUtils.getLocale("user.invalid"));
        }

        log.info("/channel/callEnd " + channelInfo);
        if (channelInfo == null) {
            return R.error(" `Invalid user channel");
        }
        //初始化通道,踢出所有人
        agoraService.suspendChannel(channelInfo.getChannel(), 0);

        //更新通道状态为关闭状态
        Boolean closed = rtcSessionCache.channelEnd(from, to, userCode.equals(from) ? 1 : 2);

        if (!closed) {
            rtcSessionCache.channelEnd(from, to, userCode.equals(from) ? 1 : 2);
            // todo 延时器预扣费
        }

        myCallDelayQueue.add(new CallDelayTask(channelInfo.getFrom(), channelInfo.getTo(), channelInfo.getChannel(), 500, userCode.equals(from) ? 1 : 2, channelInfo.getBegin()));

        if (RateLimiterCache.moreThanOnce("ce" + from + to, 3L)) {
            return R.success(MessageUtils.getLocale("result.frequent"));
        }
        //获取通道id/互动id
        String channelName = channelInfo.getChannel();
        //获取互动开始时间
        Interactive byId = interactiveService.getById(channelInfo.getBegin() + channelName);
        if (byId == null) {
            return R.error();
        }
        Long bTime = byId.getTimeMillis();
        log.info("bTime :{},{},{}", bTime, fromUser.getWallet(), toUser.getWallet());
        Long sta = System.currentTimeMillis();
        Interactive interactive = new Interactive();
        interactive.setId(channelInfo.getBegin() + channelName + "0000");
        interactive.setUserCode(from);
        interactive.setCreator(to);
        interactive.setToken(TokenEnum.APT.getSymbol());
        interactive.setAmount("");
        interactive.setCreateTime(new Date(sta).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
        interactive.setUpdateTime(new Date(sta).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
        interactive.setStatus(InteractiveStautsEnum.CALL_END.getType());
        interactive.setTimeMillis(System.currentTimeMillis());
        //记录开始和结束
        try {
            interactiveService.save(interactive);
        } catch (Exception e) {
            log.error("" + e.fillInStackTrace());
        }

        Interactive updateBegin = new Interactive();
        updateBegin.setId("" + channelInfo.getBegin() + channelName);
        updateBegin.setReserved("0000");
        updateBegin.setUpdateTime(LocalDateTime.now());
        interactiveService.updateById(updateBegin);
        return R.success();

    }


}
