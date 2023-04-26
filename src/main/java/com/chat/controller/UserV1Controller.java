package com.chat.controller;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.chat.cache.UserCache;
import com.chat.common.AuthToken;
import com.chat.common.R;
import com.chat.common.SecurityConstant;
import com.chat.config.ChatConfig;
import com.chat.config.filter.MyLocaleResolver;
import com.chat.entity.User;
import com.chat.entity.dto.UserDto;
import com.chat.entity.dto.UserWalletAuth;
import com.chat.service.AptosService;
import com.chat.service.impl.InteractiveServiceImpl;
import com.chat.service.impl.UserServiceImpl;
import com.chat.utils.JwtUtils;
import com.chat.utils.MessageUtils;
import com.chat.utils.ServletUtils;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.ip2region.core.Ip2regionSearcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author jetBrains
 * @since 2022-10-20
 */
@Slf4j
@RestController
@RequestMapping()
@AuthToken
public class UserV1Controller {

    @Resource
    private Ip2regionSearcher regionSearcher;
    @Resource
    private UserServiceImpl userService;
    @Resource
    private AptosService aptosService;

    @Resource
    private InteractiveServiceImpl interactiveService;

    @Resource
    UserCache userCache;

    @AuthToken(validate = false)
    @PostMapping("/registerv1")
    public R registerV1(@RequestBody UserWalletAuth newUser, HttpServletRequest request) throws Exception {
        String ipv4 = ServletUtils.getRealIpAddress(request);
        String region = ipv4.split("\\.").length <= 4 ? regionSearcher.getAddress(ipv4) : "";
        // String ipv4 = ServletUtils.getRealIpAddress(request);
        newUser.setIpV4(ipv4 + "#" + newUser.getIpV4());
        log.info("loginFormEmail {},ip:{}", region, ipv4);
        return userService.registerV1(newUser);
    }

    @AuthToken(validate = false)
    @PostMapping("/registerv1/end")
    public R registerV1End(UserDto newUser, @RequestParam(value = "file", required = false) MultipartFile file, HttpServletRequest request) throws Exception {
        String ipv4 = ServletUtils.getRealIpAddress(request);
        String region = ipv4.split("\\.").length <= 4 ? regionSearcher.getAddress(ipv4) : "";
        log.info("loginFormEmail {},ip:{}", region, ipv4);
        return userService.registerV1End(newUser, file, ipv4, region);
    }

    @AuthToken(validate = false)
    @PostMapping("/checkNickname")
    public boolean checkUserName(@RequestBody UserDto newUser) throws Exception {
        String userName = newUser.getUserName();
        if (StrUtil.isEmpty(userName)) {
            return false;
        }
        return userService.checkUserName(userName);
    }

    @AuthToken(validate = false)
    @PostMapping("/verifyCode")
    public R invitationCode(@RequestBody Map<String, String> map, @RequestHeader(value = "token") String token) throws Exception {

        String code = map.getOrDefault("code", "");

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
        if (StrUtil.isEmpty(userCode) || StrUtil.isEmpty(code)) {
            return R.error("error code");
        }
        User byId = userService.getById(userCode);
        if (StrUtil.isEmpty(byId.getInvitationCode())) {
            try {
                int i = Integer.parseInt(code);
                if (i == 0 || i > 5000) {
                    throw new Exception("");
                }
            } catch (Exception e) {
                return R.error("Sorry, your invitationCode is INVALID");
            }
            User user = new User();
            user.setUserCode(userCode);
            user.setInvitationCode(code);
            userService.updateById(user);
            userCache.AddOneUser(byId.getUserCode(), byId);
            if (StrUtil.isNotEmpty(byId.getAddress())) {
                userCache.AddByAddress(byId.getAddress(), byId);
            }
            JSONArray userAssets = aptosService.getUserAssets(byId.getAddress());
            if (userAssets != null && userAssets.size() > 0) {
                if (userAssets.getLong(0) == 0 || userAssets.getLong(1) == 0) {
                    aptosService.dat3SysUserInit(code, userCode, byId.getAddress());
                    aptosService.addInvitee(code, byId.getAddress());


                }
            }
            return R.success();
        }
        return R.success("You have verified the code :" + byId.getInvitationCode());
    }


    @AuthToken(validate = false)
    @GetMapping("/verifyInit")
    public R invitationCode(@RequestHeader(value = "token") String token) throws Exception {


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
        if (StrUtil.isEmpty(userCode)) {
            return R.error("error code");
        }
        User user = userService.getById(userCode);
        if (user != null) {
            JSONArray userAssets = aptosService.getUserAssets(user.getAddress());
            String code = "0";
            if (userAssets != null && userAssets.size() > 0) {
                try {
                    int i = Integer.parseInt(user.getInvitationCode());
                    if (i == 0 || i > 5000) {
                        throw new Exception("");
                    }
                    code = "" + i;
                } catch (Exception e) {
                    return R.error("Sorry, your invitationCode is INVALID");
                }
                if (userAssets.getLong(0) == 0 || userAssets.getLong(1) == 0) {
                    aptosService.dat3SysUserInit(code, userCode, user.getAddress());
                    aptosService.addInvitee(code, user.getAddress());
                }
            }
        }

        return R.success();
    }

    private static TimedCache<String, JSONArray> FID_REWARD = CacheUtil.newTimedCache(60 * 1000);

    @AuthToken(validate = false)
    @GetMapping("/inviteUsers/{fid}")
    public R inviteUsers(@PathVariable String fid, @RequestParam(required = false) Integer size, @RequestParam(required = false) Integer page) throws Exception {
        try {
            if (StrUtil.isEmpty(fid)
                    || Integer.parseInt(fid) < 1
                    || Integer.parseInt(fid) > 5040) {
                return R.error("Sorry, your invitationCode is INVALID");
            }

        } catch (Exception e) {
            return R.error("Sorry, your invitationCode is INVALID");
        }
        if (size == null || size == 0) {
            size = 100;
        }
        if (page == null || page == 0) {
            page = 1;
        }
        JSONArray fid_reward = FID_REWARD.get(fid);
        if (fid_reward == null) {
            fid_reward = aptosService.fidReward(fid, page, size);
            if (fid_reward != null) {
                FID_REWARD.put(fid, fid_reward);
            }
        }
        if (fid_reward != null && fid_reward.getInt(0, 0) != 0) {
            JSONObject re = new JSONObject();
            re.set("inviteCode", fid_reward.getStr(0));
            re.set("inviteReward", fid_reward.getStr(1));
            re.set("spendReward", fid_reward.getStr(2));
            re.set("earnReward", fid_reward.getStr(3));
            JSONArray invitee = fid_reward.getJSONArray(4);

            int total = fid_reward.getInt(5);


            LinkedList data = new LinkedList<>();
            for (Object object : invitee) {
                String addr = object.toString();
                User user = (User) userCache.getByAddress(addr);
                HashMap<Object, Object> map = new HashMap<>();
                map.put("address", addr);
                map.put("nickname", addr.substring(0, addr.length() - (addr.length() - 6)) + "****" + addr.substring(addr.length() - 4));
                map.put("portrait", "");
                map.put("userCode", "");
                if (user != null) {
                    map.put("nickname", user.getUserName());
                    map.put("portrait", user.getPortrait());
                    map.put("userCode", user.getUserCode());
                }
                data.add(map);
            }
            JSONObject res = new JSONObject();
            res.set("page", page);
            res.set("size", size);
            res.set("data", data);
            res.set("total", total);
            re.set("inviteePage", res);

            re.set("alreadyClaim", fid_reward.getStr(5));
            re.set("allReward", "" + (re.getLong("inviteReward") + re.getLong("spendReward")));
            return R.success(re);
        } else {
            JSONObject re = new JSONObject();
            re.set("inviteCode", fid);
            re.set("inviteReward", "0");
            re.set("spendReward", "0");
            re.set("earnReward", "0");
            JSONObject res = new JSONObject();
            res.set("page", page);
            res.set("size", size);
            res.set("data", new JSONArray());
            res.set("total", 0);
            re.set("inviteePage", res);

            re.set("alreadyClaim", "0");
            re.set("allReward", "0");
            return R.success(re);
        }
    }


    @GetMapping("/share")
    public R share(@RequestHeader(value = "token") String token) throws Exception {
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
        User user = userCache.getUser(userCode);
        if (user == null) {
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        return R.success(ChatConfig.HOME + "/" + user.getAddress());

    }



    @AuthToken(validate = false)
    @PostMapping("/getReceiver")
    public R getReceiver(@RequestBody List<String> list, @RequestHeader(value = "token") String token) throws Exception {


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
        JSONArray res=new JSONArray();
        if (CollUtil.isNotEmpty(list)) {
            for (String s : list) {
                HashMap map = new HashMap();
                map.put("userCode", s);
                if (StrUtil.isNotEmpty(s)) {
                    map.put("address", "");
                    map.put("nickname", "");
                    map.put("portrait", "");
                    User user = userCache.getUser(s);
                    if (user!=null) {
                        map.put("address", user.getAddress());
                        map.put("nickname", user.getUserName());
                        map.put("portrait", user.getPortrait());
                    }
                }
                res.add(map);
            }
            return R.success(res);
        }

        return R.error();
    }
}
