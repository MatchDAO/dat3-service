package com.chat.service;

import cn.hutool.core.collection.BoundedPriorityQueue;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.chat.cache.UserCache;
import com.chat.common.R;
import com.chat.common.ResultCode;
import com.chat.common.SecurityConstant;
import com.chat.common.TokenConstant;
import com.chat.config.UncheckUserProperties;
import com.chat.entity.dto.CaptchaDto;
import com.chat.entity.dto.TokenDto;
import com.chat.entity.MailRequest;
import com.chat.pool.CoreThreadPool;
import com.chat.service.impl.EmailServiceImpl;
import com.chat.utils.JwtUtils;
import com.chat.utils.RedisUtil;
import com.chat.utils.StringUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Component
public class TokenService {

    @Resource
    UserCache userCache;
    @Resource
    private RedisUtil redisUtils;
    @Resource
    private EmailServiceImpl emailService;
    @Resource
    private UncheckUserProperties uncheckUserProperties;
    /**
     * 设置过期时间 10分钟 这个后面要移动到统一枚举类里
     */
    private final static long expireTime = 24 * 60L;
    /**
     * 设置过期时间 10分钟 这个后面要移动到统一枚举类里
     */
    private final static long captchaTime = 10 * 60L;


    //本地缓存
    BoundedPriorityQueue<HashMap> queue = new BoundedPriorityQueue<>(10, new Comparator<HashMap>() {
        @Override
        public int compare(HashMap o1, HashMap o2) {
            return (((Date) (o2.get("time"))).compareTo(((Date) (o1.get("time")))));
        }
    });


    /**
     * 创建token jwt
     */
    public Map<String, Object> createToken(TokenDto tokenDto) {
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put(SecurityConstant.USER_ID, tokenDto.getUserId());
        claimsMap.put(SecurityConstant.USER_ACCOUNT, tokenDto.getUserAccount());
        claimsMap.put(SecurityConstant.USER_TYPE, tokenDto.getUserType());
        claimsMap.put(SecurityConstant.ADDRESS, tokenDto.getAddress());
        if (tokenDto.getExpireTime()==0) {
            tokenDto.setExpireTime(TokenConstant.EXPIRE_TIME);
        }


        String token = JwtUtils.CreateToken(claimsMap ,tokenDto.getExpireTime());
        tokenDto.setToken(token);
        Map<String, Object> resMap = new HashMap<>();
        resMap.put("token", token);
        resMap.put("expires_in", tokenDto.getExpireTime());
        refreshToken(tokenDto);
        return resMap;
    }
    public Map<String, Object> createTokenv1(TokenDto tokenDto) {
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put(SecurityConstant.USER_ID, tokenDto.getUserId());
        claimsMap.put(SecurityConstant.USER_ACCOUNT, tokenDto.getUserAccount());
        claimsMap.put(SecurityConstant.USER_TYPE, tokenDto.getUserType());
        claimsMap.put(SecurityConstant.ADDRESS, tokenDto.getAddress());
        if (tokenDto.getExpireTime()==0) {
            tokenDto.setExpireTime(TokenConstant.EXPIRE_TIME);
        }


        String token = JwtUtils.CreateToken(claimsMap ,tokenDto.getExpireTime());
        tokenDto.setToken(token);
        Map<String, Object> resMap = new HashMap<>();
        resMap.put("token", token);
        resMap.put("expires_in", tokenDto.getExpireTime());
        refreshToken(tokenDto);
        return resMap;
    }

    /**
     * 解析token数据
     */
    public Map<String, Object> parseToken(String token) {
        Map<String, Object> rsMap = null;
        try {
            rsMap = JwtUtils.parseToken(token);
        } catch (Exception e) {
            log.error("parseToken error:" + e.fillInStackTrace());
        }

        return rsMap;
    }

    public boolean checkToken(String token) {
        Claims claims = null;
        try {
            claims = JwtUtils.parseToken(token);
        } catch (Exception e) {
            log.error("checkToken error:" + e.fillInStackTrace());
        }

        if (claims == null) {
            return false;
        }
        String account = JwtUtils.getUserId(claims);
        String type = JwtUtils.getUserType(claims);
        if (redisUtils.hasKey("sso:token:" + type + ":" + account)) {
            TokenDto dto = (TokenDto) redisUtils.get("sso:token:" + type + ":" + account);
            if (dto != null && token.equalsIgnoreCase(dto.getToken())) {
                return true;
            }
        }
        return false;
    }

    public TokenDto getTokenEntity(String token) {
        Claims claims = null;
        try {
            claims = JwtUtils.parseToken(token);
        } catch (Exception e) {
            log.error("checkToken error:" + e.fillInStackTrace());
        }
        if (claims == null) {
            return null;
        }
        Object o = redisUtils.get("sso:token:" + JwtUtils.getUserType(claims) + ":" + JwtUtils.getUserId(claims));
        if (o != null) {
            return (TokenDto) o;
        }
        return null;
    }

    public boolean hasTokenNoCheck(TokenDto tokenDto) {
        return redisUtils.hasKey("sso:token:" + tokenDto.getUserType() + ":" + tokenDto.getUserAccount());
    }


    private void refreshToken(TokenDto tokenDto) {
        redisUtils.set("sso:token:" + tokenDto.getUserType() + ":" + tokenDto.getUserId(), tokenDto, tokenDto.getExpireTime());
    }

    public void evictToken(TokenDto tokenDto) {
        redisUtils.del("sso:token:" + tokenDto.getUserType() + ":" + tokenDto.getUserAccount());
    }

    /**
     * 新增验证码,缓存
     * 当前用户验证码不存在，写入值, 并返回true; 当前key已经存在，不处理, 返回false;
     *
     * @param captchaDto c
     * @return e
     */
    public String refreshCaptcha(CaptchaDto captchaDto) throws Exception {
        String phoneCode = captchaDto.getPhoneCode();
        String account = captchaDto.getAccount();
        String k = "sso:captcha:" + captchaDto.getType() + ":" + account;
        if (captchaDto.getExpireTime() - redisUtils.getExpire(k) < 60) {
            return "Sending verification code too frequently, please try again later";
        }
        //白名单固定1234
        List<String> whites = uncheckUserProperties.getWhites();

        boolean isWhites =false;
        if (StringUtil.matches(account, whites)) {
            isWhites=true;
            captchaDto.setCaptcha("1234");
        }else
        if ( userCache.isInternal(account)) {
            isWhites=true;
            captchaDto.setCaptcha("202212");

        }
        //   缓存到redis
        if (redisUtils.set(k, captchaDto, captchaDto.getExpireTime())) {
            if (captchaDto.getAccountType() != null && captchaDto.getAccountType() == 1) {
                MailRequest mailRequest = new MailRequest();
                mailRequest.setSubject("Your DAT3 Code - " + captchaDto.getCaptcha());
                mailRequest.setSendTo(captchaDto.getAccount());
                mailRequest.setText("Your DAT3 code is: " + captchaDto.getCaptcha() + ". Use it to verify your email and start your journey in dat3.");
                if (!isWhites) {
                    CoreThreadPool.execute("sendVerifyCode",()->emailService.sendSimpleMail(mailRequest));
                }
                log.info("refreshCaptcha {}", captchaDto);
            }
            //todo phone
        }
        final HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put("captchaDto", captchaDto);
        objectObjectHashMap.put("time", new Date());
        queue.offer(objectObjectHashMap);
        return "success";
    }

    /**
     * 获取验证码
     *
     * @param captchaDto
     * @return
     */
    public CaptchaDto parseCaptcha(CaptchaDto captchaDto) {
        Boolean aBoolean = redisUtils.hasKey("sso:captcha:" + captchaDto.getType() + ":" + captchaDto.getAccount());
        if (aBoolean) {
            return (CaptchaDto) redisUtils.get("sso:captcha:" + captchaDto.getType() + ":" + captchaDto.getAccount());
        }
        return new CaptchaDto();
    }


}
