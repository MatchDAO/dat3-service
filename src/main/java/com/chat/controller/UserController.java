package com.chat.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chat.cache.RateLimiterCache;
import com.chat.common.AuthToken;
import com.chat.common.CaptchaTypeEnum;
import com.chat.common.R;
import com.chat.common.SecurityConstant;
import com.chat.config.ChatConfig;
import com.chat.entity.User;
import com.chat.entity.dto.CaptchaDto;
import com.chat.entity.dto.UserAuthDto;
import com.chat.entity.dto.UserDto;
import com.chat.service.EmTemplate;
import com.chat.service.TokenService;
import com.chat.service.impl.UserServiceImpl;
import com.chat.utils.JwtUtils;
import com.chat.utils.MessageUtils;
import com.chat.utils.RedisUtil;
import com.chat.utils.ServletUtils;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.ip2region.core.Ip2regionSearcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
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
public class UserController {

    @Resource
    private Ip2regionSearcher regionSearcher;
    @Resource
    private UserServiceImpl userService;
    @Resource
    private TokenService tokenService;
    @Resource
    private EmTemplate emTemplate;

    @Resource
    private RedisUtil redisUtil;

    @AuthToken(validate = false)
    @PostMapping("/sendMessage")
    public R sendMessage(@RequestBody CaptchaDto captchaDto, HttpServletRequest request) throws Exception {

        String code = RandomUtil.randomString("0123456789", 6);
        String account = captchaDto.getAccount();
        String subject = "Verification code from xxxx";
        //todo 与前端约束type
        CaptchaTypeEnum typeEnum = CaptchaTypeEnum.of(captchaDto.getType());
        if (CaptchaTypeEnum.LOGIN.equals(typeEnum)) {
            subject = "Verification code from xxxx";
        }
        if (CaptchaTypeEnum.MODIFY.equals(typeEnum)) {
            subject = "Verification code from xxxx";
        }
        if (CaptchaTypeEnum.REGISTER.equals(typeEnum)) {
            subject = "Verification code from xxxx";
        }
        if (CaptchaTypeEnum.LOGIN_REGISTER.equals(typeEnum)) {
            subject = "Verification code from xxxx";
        }

        if (RateLimiterCache.moreThanOnce(account + typeEnum.getType(), 60)) {
            return R.success("Failed to send a verification code, please try it again");
        }

        // 发送邮箱验证码
        try {
            CaptchaDto captchaDto1 = new CaptchaDto();
            captchaDto1.setAccount(account);
            //  验证码类型  CaptchaTypeEnum
            captchaDto1.setType(typeEnum.getSign());
            //过期时间
            captchaDto1.setExpireTime(300L);
            captchaDto1.setAccountType(1);
            captchaDto1.setSubject(subject);
            captchaDto1.setCaptcha(code);
            String msg = tokenService.refreshCaptcha(captchaDto1);
            if (!"success".equals(msg)) {
                return R.error(msg);
            }
            return R.success("Successfully send the message");
        } catch (MessagingException e) {
            log.error("[EmailloginAndRegisterController.sendEmail]:" + e.fillInStackTrace());
        }
        return R.error("Failed to send a verification code, please try it again");

    }

    @AuthToken(validate = false)
    @PostMapping("/register/login")
    public R register(@RequestBody UserAuthDto newUser, HttpServletRequest request) throws Exception {
        String ipv4 = ServletUtils.getRealIpAddress(request);
        String region = ipv4.split("\\.").length <= 4 ? regionSearcher.getAddress(ipv4) : "";
        // String ipv4 = ServletUtils.getRealIpAddress(request);
        newUser.setIpV4(ipv4);
        newUser.setRegions(region);
        log.info("loginFormEmail {},ip:{}", region, ipv4);
        return userService.register(newUser, "login");
    }

    @AuthToken(validate = false)
    @PostMapping("/register/end")
    public R registerEnd( UserDto newUser, @RequestParam(value = "file", required = false) MultipartFile file , HttpServletRequest request) throws Exception {
        String ipv4 = ServletUtils.getRealIpAddress(request);
        String region = ipv4.split("\\.").length <= 4 ? regionSearcher.getAddress(ipv4) : "";
        log.info("loginFormEmail {},ip:{}", region, ipv4);
        return userService.registerEnd(newUser,file,ipv4,region);
    }

//    public R modifyUserInfo(UserDto newUser, @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {
    @AuthToken(validate = false)
    @PostMapping("/login")
    public R login(@RequestBody UserAuthDto newUser, HttpServletRequest request) throws Exception {
        return userService.login(newUser);
    }

    @GetMapping("/user/delAccount")
    public R delAccount(@RequestHeader(value = "token") String token)  {

        String email = null;
        try {
            email = JwtUtils.getUser(token, SecurityConstant.USER_ACCOUNT);
            if (email == null) {
                return R.success(0);
            }
        } catch (Exception e) {
            log.error("`Invalid user" + e.fillInStackTrace());
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        R r = userService.logout(email);

        return R.success("Your account has been safely del  ");
    }

    @GetMapping("/user/logout")
    public R logout(@RequestHeader(value = "token") String token)  {

        String email = null;
        try {
            email = JwtUtils.getUser(token, SecurityConstant.USER_ACCOUNT);
            if (email == null) {
                return R.success(0);
            }
        } catch (Exception e) {
            log.error("`Invalid user" + e.fillInStackTrace());
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        R r = userService.logout(email);

        return r;
    }

    @PostMapping("/user/modify")
    public R modifyUserInfo(UserDto newUser, @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {
        return userService.modifyUserInfo(newUser, file);
    }

    @PostMapping("/user/{userCode}/portrait")
    public R modifyUserPortrait(@PathVariable String userCode, @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {
        return userService.modifyUserPortrait(userCode, file);
    }

    @AuthToken(validate = false)
    @PostMapping("/user/info")
    public R userInfo(@RequestBody UserDto newUser) {
        return  userService.getUser(newUser) ;
    }


    //刷新token


    @PostMapping("/refreshEmToken")
    public R refreshEmToken(@RequestBody UserAuthDto newUser) {

        List<User> list = userService.list(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, newUser.getAccount())
                .or()
                .eq(User::getEmail, newUser.getAccount()).last("limit 1"));
        if (CollectionUtil.isEmpty(list)) {
            return R.error("no such user:" + newUser.getAccount());
        }
        User user = list.get(0);
        JSONObject entries = JSONUtil.createObj();
        entries.put("emToken", emTemplate.getUserToken(user.getUserCode(), user.getEmUuid(), 0, ChatConfig.EM_PWD + user.getUserCode()));
        entries.put("userCode", user.getUserCode());
        return R.success(entries);
    }
    @PostMapping("/user/updateAddress")
    public R updateAddress(@RequestBody Map<String,String> req, @RequestHeader(value = "token") String token)  {
        String address = req.getOrDefault("address", "");
        if (StrUtil.isEmpty(address)) {
            return R.error("`Invalid user address");
        }
        address=address.trim().toLowerCase();
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
        User user = userService.getById(userCode);
        if (user==null) {
            return R.error("no such user:" + userCode);
        }
        if (address.equalsIgnoreCase(user.getAddress())) {
            return R.success();
        }
        User user1 = new User();
        user1.setUserCode(userCode);
        user1.setAddress(address);
        boolean b = userService.updateById(user1);
        if (!b) {
            return R.error("error update:" );
        }

        return R.success();
    }
}
