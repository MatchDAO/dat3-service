package com.chat.controller;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.chat.cache.UserCache;
import com.chat.common.AuthToken;
import com.chat.common.BotCodeEnum;
import com.chat.common.R;
import com.chat.entity.User;
import com.chat.entity.dto.InteractiveDto;
import com.chat.service.AptosService;
import com.chat.service.EmTemplate;
import com.chat.service.EmailService;
import com.chat.utils.JwtUtils;
import com.chat.utils.MessageUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.web3j.abi.datatypes.Address;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import static com.chat.utils.UserIdUtils.fillStr;

@Slf4j
@RestController
@AuthToken
@RequestMapping("/bot")
public class BotController {

    @Resource
    private EmTemplate emTemplate;
    Cache<String, LinkedHashMap<String, Long>> cache = Caffeine.newBuilder()
            .expireAfterAccess(3600, TimeUnit.SECONDS)
            .maximumSize(100000)
            .build();
    Cache<String, JSONArray> mintCache = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .maximumSize(5600)
            .build();

    @Resource
    private UserCache userCache;
    @Resource
    private AptosService aptosService;

    @AuthToken(validate = false)
    @PostMapping("/sendMsg")
    public R sendMsg(@RequestBody HashMap<String, String> map, @RequestHeader(value = "token") String token) {
        String userCode = null;
        try {
            userCode = JwtUtils.getUserKey(token);
        } catch (Exception e) {
            log.error("`Invalid user" + e.fillInStackTrace());
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        User user = userCache.getUser(userCode);
        if (user == null || StrUtil.isEmpty(user.getAddress())) {
            return R.error(MessageUtils.getLocale("user.invalid"));
        }

        String code = map.getOrDefault("code", "");
        code = code.toLowerCase();
        BotCodeEnum of = BotCodeEnum.of(code);
        if (of.equals(BotCodeEnum.UNKNOWN)) {
            return R.success();
        }
        JSONObject res = new JSONObject();
        res.set("msg", "");

        if (of.equals(BotCodeEnum.MINT)) {
            LinkedHashMap<String, Long> mintCodes = cache.get(userCode, (k) -> {
                LinkedHashMap<String, Long> temp1 = new LinkedHashMap<>();
                temp1.put(of.getCode(), System.currentTimeMillis());
                return temp1;
            });
            JSONArray nft = mintCache.get(userCache.getUser(userCode).getAddress(), (k) -> aptosService.getNftMintState(k));
            if (nft == null) {
                return R.error();
            }

            //第一次mint
            LinkedHashMap<String, Long> temp = new LinkedHashMap<>();
            temp.put(of.getCode(), System.currentTimeMillis());
            cache.put(userCode, temp);
            Long quantity = nft.getLong(1);
            Long start_time = nft.getLong(3);
            Long end_time = nft.getLong(4);
            Long progress = nft.getLong(5);
            Boolean inWhitelist = nft.getBool(6);
            Long mint_num = nft.getLong(7, 0L);
            JSONArray mint_nft = nft.getJSONArray(8);
            boolean start = System.currentTimeMillis() / 1000 > start_time;
            res.set("start", start);
            if (mint_num < 1 && inWhitelist && start) {

                String msg = "You can mint ,proceed if you:\n" +
                        "✅ are on DAT3 freemint whitelist\n" +
                        "✅ have no DAT3 invitation NFT\n" +
                        "✅ have sufficient poor balance for gas fee";

                res.set("mint_num", mint_num);
                res.set("inWhitelist", inWhitelist);
                res.set("start_time", start_time * 1000);
                res.set("msg", msg);
                emTemplate.messageSend("30416523", userCode, msg);

                return R.success(res);
            }
            if (!inWhitelist) {
                String msg = "We notice you're not on whitelist. We're sorry to inform you that only whitelisted users are qualified for mint. \n" +
                        "For whitelist spots, please check events on our social media accounts. \n" +
                        "Twitter:@chatdat3\n" +
                        "Discord:discord.gg/yD447YwBve\n" +
                        "Mail:matchdao.web3@gmail.com";
                res.set("mint_num", mint_num);
                res.set("inWhitelist", inWhitelist);
                res.set("start_time", start_time * 1000);
                msg = "You're not on whitelist.";
                res.set("msg", msg);
                emTemplate.messageSend("30416523", userCode, msg);
                return R.success(res);
            }
            if (mint_num > 0) {
                String msg = "We are delighted by your strong interest in DAT3 invitation NFTs. You can now earn invitation rewards by inviting friends with your current code.\n" +
                        "\n" +
                        "To better serve our users, each person can only mint once. If you want more NFTs, please refer to future social media events or trade in NFT marketplaces.";

                res.set("mint_num", mint_num);
                res.set("inWhitelist", inWhitelist);
                res.set("start_time", start_time * 1000);
                msg = "You have successfully Minted,You can now earn invitation rewards by inviting friends with your current code.If you want more NFTs, please refer to future social media events or trade in NFT marketplaces.";
                res.set("msg", msg);
                emTemplate.messageSend("30416523", userCode, msg);
                return R.success(res);
            }

            if (!start) {
                LocalDateTime of1 = LocalDateTimeUtil.of(System.currentTimeMillis());
                LocalDateTime of2 = LocalDateTimeUtil.of(start_time * 1000);
                String msg = "Mint has not yet opened. Please come back in " + getDate(of1, of2);
                res.set("msg", msg);
                res.set("inWhitelist", inWhitelist);
                res.set("start_time", start_time * 1000);
                res.set("mint_num", mint_num);
                emTemplate.messageSend("30416523", userCode, msg);
                return R.success(res);
            }


        }
        if (of.equals(BotCodeEnum.CONFIRM)) {
            LinkedHashMap<String, Long> mintCodes = cache.get(userCode, (k) -> {
                LinkedHashMap<String, Long> temp1 = new LinkedHashMap<>();
                temp1.put(of.getCode(), System.currentTimeMillis());
                return temp1;
            });
            JSONArray nft = mintCache.get(userCache.getUser(userCode).getAddress(), (k) -> aptosService.getNftMintState(k ));
            if (nft == null) {
                return R.error();
            }
            Long quantity = nft.getLong(1);
            Long start_time = nft.getLong(3);
            Long end_time = nft.getLong(4);
            Long progress = nft.getLong(5);
            Boolean inWhitelist = nft.getBool(7);
            Long mint_num = nft.getLong(8, 0L);
            JSONArray mint_nft = nft.getJSONArray(9);
            res.set("mint_num", mint_num);
            res.set("inWhitelist", inWhitelist);
            res.set("start_time", start_time * 1000);

            if (mint_num > 0) {
                String msg = "We are delighted by your strong interest in DAT3 invitation NFTs. You can now earn invitation rewards by inviting friends with your current code.\n" +
                        "\n" +
                        "To better serve our users, each person can only mint once. If you want more NFTs, please refer to future social media events or trade in NFT marketplaces.";
                emTemplate.messageSend("30416523", userCode, msg);
                res.set("msg", msg);
            }
            return R.success(res);
        }

        return R.success(res);
    }


    public static String getDate(LocalDateTime now, LocalDateTime end) {
        System.out.println(now + "||||" + end);
        System.out.println("计算两个时间的差：");
        //获取秒数
        long nowSecond = now.toEpochSecond(ZoneOffset.ofHours(0));
        long endSecond = end.toEpochSecond(ZoneOffset.ofHours(0));
        long absSeconds = Math.abs(nowSecond - endSecond);
        System.out.println(nowSecond + "||" + endSecond);
        //获取秒数
        long s = absSeconds % 60;
        //获取分钟数
        long m = absSeconds / 60 % 60;
        //获取小时数
        long h = absSeconds / 60 / 60 % 24;
        //获取天数
        long d = absSeconds / 60 / 60 / 24;

        return fillStr(d + "", '0', 2, true) + ":"
                + fillStr(h + "", '0', 2, true) + ":"
                + fillStr(m + "", '0', 2, true);
        //  + fillStr(s+"",'0',2,true) ;
    }

}
