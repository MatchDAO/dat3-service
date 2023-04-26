package com.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.img.Img;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.cache.RateLimiterCache;
import com.chat.cache.UserCache;
import com.chat.common.*;
import com.chat.config.ChatConfig;
import com.chat.entity.Creator;
import com.chat.entity.InvitationRewards;
import com.chat.entity.User;
import com.chat.entity.dto.*;
import com.chat.entityMapper.User2DtoMapper;
import com.chat.mapper.UserMapper;
import com.chat.service.*;
import com.chat.task.queue.MsgDelayTask;
import com.chat.utils.*;
import com.easemob.im.server.model.EMUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.DelayQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.chat.service.impl.SysFileServiceImpl.awsUrl;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jetBrains
 * @since 2022-10-20
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private CustomMsgServiceImpl customMsgService;
    @Resource
    private UserIdUtils userIdUtils;
    @Resource
    private UserCache userCache;
    @Resource
    private CreatorServiceImpl creatorService;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private TokenService tokenService;
    @Resource
    private EmTemplate emTemplate;
    @Resource
    private TransactionUtils transactionUtils;

    @Resource
    private SysFileService sysFileService;
    @Resource
    private SysFileServiceImpl sysFileService1;
    @Resource
    private InvitationRewardsService invitationRewardsService;

    @Resource
    private InvitationCodeTotalServiceImpl invitationCodeTotalService;


    @Resource
    private DelayQueue<MsgDelayTask> myMsgDelayQueue;

    @Resource
    private AptosService aptosService;

    @Override
    public R register(UserAuthDto newUser, String login) throws Exception {


        log.info("register,{}", newUser);
        //获取验证码
        String code = newUser.getCaptcha();
        //密码
        // String password = newUser.getPassword();
        String account = StrUtil.isEmpty(newUser.getAccount()) ? "" : newUser.getAccount().trim();
        String invitationCode = StrUtil.isEmpty(newUser.getInvitation()) ? null : newUser.getInvitation().trim();

        CaptchaDto captchaDto = new CaptchaDto();
        captchaDto.setAccount(account);
        //获取验证码类型
        String captchaType = CaptchaTypeEnum.REGISTER.getSign();
        if ("login".equals(login)) {
            captchaType = CaptchaTypeEnum.LOGIN_REGISTER.getSign();
        }
        captchaDto.setType(captchaType);
        //验证失败次数大于设定阈值
        if (userCache.validationFailed("login" + account) >= ChatConfig.MAX_VALIDATION_FAILED_TIMES) {
            return R.error("Sorry, your verification failed more than " + ChatConfig.MAX_VALIDATION_FAILED_TIMES + " times today");
        }
        //限流
        if (RateLimiterCache.moreThanOnce(account + "login", 3)) {
            return R.error("Sorry, your Login too frequently");
        }

        if (!userCache.isInternal(account)) {
            CaptchaDto captcha = tokenService.parseCaptcha(captchaDto);
            //进行验证码的比对（页面提交的验证码和redis中保存的验证码比对）
            if (captcha == null
                    || captcha.getCaptcha() == null
                    || !captcha.getCaptcha().equals(code)) {
                //验证失败次数+1
                userCache.addValidationFailed("login" + account);
                return R.custom(ResultCode.FORBIDDEN.getCode(), "The verification code is incorrect or expired");
            }
        } else {
            if (!"202212".equals(code.trim())) {
                return R.custom(ResultCode.FORBIDDEN.getCode(), "The verification code is incorrect or expired");

            }
        }
        // 从Redis中获取缓存验证码

        User user = new User();
        final List<User> list = list(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, newUser.getAccount().trim())
                .or()
                .eq(User::getEmail, newUser.getAccount().trim()));
        Long t1, t2 = 0L, t3;
        //新增加用户
        boolean save = false;
        if (CollectionUtil.isEmpty(list)) {
            String portrait = "default";
            String lestUserCode = userIdUtils.getNewUserCode("");

            if (StrUtil.isEmpty(invitationCode)) {
                return R.custom(ResultCode.NOT_FOUND.getCode(), "The invitation code does not exist.");
            }
            try {
                String s = "";
                try {
                    s = InvitationCodeUtil.decodeUserCode(invitationCode);
                    log.info("invitationCode:{},s:{},i:{}", invitationCode, s, InvitationCodeUtil.decodeIndex(invitationCode));
                } catch (Exception e) {
                    log.error("`Invalid invitation code .");
                    return R.error("`Invalid invitation code .");
                }


                List<User> users = this.list(new LambdaQueryWrapper<User>().eq(User::getUserCode, s));
                List<InvitationRewards> list1 = invitationRewardsService.list(new LambdaQueryWrapper<InvitationRewards>().eq(InvitationRewards::getInvitationCode, invitationCode));
                if (CollUtil.isEmpty(users) || !CollUtil.isEmpty(list1)) {
                    return R.custom(ResultCode.NOT_FOUND.getCode(), "The invitation code does not exist.");
                }
                invitationCodeTotalService.changeTotal(ActionEnum.SIGN_IN.getSign(), 1, s, 1);
            } catch (Exception e) {
                log.error("" + e);
                e.fillInStackTrace();

            }
            user.setInvitationCode(invitationCode);
//            Mac mac = new HMac(HmacAlgorithm.HmacSHA1, lestUserCode.getBytes());
//            String s1 = mac.digestHex(password);
            user.setUserName(StringUtil.desensitized(account, '@', 3));
            // user.setPassword(s1);
            user.setEmail(account);
            user.setPortrait(portrait);
            user.setUserCode(lestUserCode);
            user.setRegistered(0);
            user.setGender(0);
            user.setCreatedTime(LocalDateTime.now());
            save = save(user);
            if (!save) {
                return R.custom(ResultCode.FAILED.getCode(), ResultCode.FAILED.getMessage());
            }

            UserDto userDto = User2DtoMapper.INSTANCE.user2Dto(user);
            userDto.setFirstLogin(true);
            JSONObject entries = JSONUtil.parseObj(userDto);
            return R.success("login_success", entries);
        } else {
            user = list.get(0);
        }
        Integer registered = user.getRegistered();
        if (1 != registered) {
            UserDto userDto = User2DtoMapper.INSTANCE.user2Dto(user);
            userDto.setFirstLogin(true);
            JSONObject entries = JSONUtil.parseObj(userDto);
            return R.success("login_success", entries);
        }

        String emToken = emTemplate.getUserToken(user.getUserCode(), user.getEmUuid(), 0, ChatConfig.EM_PWD + user.getUserCode());
        t3 = System.currentTimeMillis();
        log.info("t3-t2:{}", t3 - t2);
        UserDto userDto = User2DtoMapper.INSTANCE.user2Dto(user);
        userDto.setFirstLogin(false);
        JSONObject entries = JSONUtil.parseObj(userDto);
        entries.put("emToken", emToken);
        //注册登录
        if ("login".equals(login)) {
            TokenDto tokenDto = new TokenDto();
            tokenDto.setUserId(user.getUserCode());
            tokenDto.setRegion(newUser.getRegions());
            tokenDto.setIpaddr(newUser.getIpV4());
            tokenDto.setLast(new Date());
            tokenDto.setUserType("member");
            tokenDto.setUserAccount(user.getEmail());
            tokenDto.setExpireTime(TokenConstant.EXPIRE_TIME);
            Map<String, Object> token = tokenService.createToken(tokenDto);
            //新增缓存
            userCache.AddOneUser(user.getUserCode(), user);
            if (StrUtil.isNotEmpty(user.getAddress())) {
                userCache.AddByAddress(user.getAddress(), user);
            }
            userCache.AddByAddress(user.getAddress(), user);
            entries.putAll(token);
            entries.set("sayHi", "[]");
            boolean firstLogin = redisUtil.hHasKey(RedisKeys.USER + ":inv:l" + DateUtils.getSimpleToday(), user.getUserCode());
            log.info("firstLogin:{}", firstLogin);
            if (!firstLogin) {
                List<HashMap<String, String>> customMsg = customMsgService.getCustomMsg(user.getUserCode(), user.getUserName());
                if (!CollUtil.isEmpty(customMsg)) {
                    for (HashMap<String, String> map : customMsg) {
                        String reply = map.getOrDefault("reply", "");
                        String cuserCode = map.getOrDefault("cuserCode", "");
                        //log.info("cuserCode:{},{},{}",firstLogin,user.getUserCode(),reply);
                        //  myMsgDelayQueue.add(new MsgDelayTask(user.getUserCode(), cuserCode, reply, 1000L * (RandomUtil.randomInt(10, 100))));
                    }
                }
                entries.set("sayHi", JSONUtil.parseArray(customMsg));
            }
            invitationCodeTotalService.changeTotal(ActionEnum.SIGN_IN.getSign(), 1, user.getUserCode(), 0);
            log.info(user.getEmail() + "   " + entries);
            return R.success("login_success", entries);
        }
        if (CollectionUtil.isEmpty(list)) {
            return R.custom(ResultCode.FAILED.getCode(), ResultCode.FAILED.getMessage());
        } else {
            return R.custom(ResultCode.SUCCESS.getCode(), "Already registered", entries);
        }
    }

    @Override
    public R registerEnd(UserDto query, MultipartFile file, String ipv4, String region) {

        User byId = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUserCode, query.getUserCode())
                .eq(User::getRegistered, 0));
        if (byId == null) {
            return R.error("Failed to register");
        }
        LambdaUpdateWrapper<User> set = new LambdaUpdateWrapper<User>()
                .eq(User::getUserCode, query.getUserCode());

        if (!StrUtil.isEmpty(query.getUserName())) {
            set.set(User::getUserName, query.getUserName());
        }
        if (!StrUtil.isEmpty(query.getBio())) {
            set.set(User::getBio, query.getBio());
        }
        if (query.getGender() != null && query.getGender() >= 0 && query.getGender() < 4) {
            set.set(User::getGender, query.getGender());
        } else {
            set.set(User::getGender, 0);
        }

        if (file == null || file.isEmpty()) {
            return R.error("Failed to upload portrait");
        }
        String portrait = byId.getPortrait();
        long size = file.getSize();
        String tempFileName = ChatConfig.RESOURCES_PATH + System.currentTimeMillis() + file.getOriginalFilename();
        Map<String, Object> stringStringMap = null;
        if (size < 254694) {
            stringStringMap = sysFileService.uploadFile(file, query.getUserCode(), 0);

        } else {
            try {

                boolean write = Img.from(file.getInputStream()).setQuality(0.8F).write(new File(tempFileName));
                if (write) {
                    stringStringMap = sysFileService.uploadFile(Files.newInputStream(Paths.get(tempFileName)), file.getOriginalFilename(), file.getName(), file.getContentType(), query.getUserCode());
                }
            } catch (Exception e) {
                e.fillInStackTrace();
            } finally {
                File file1 = new File(tempFileName);
                file1.setWritable(true, false);
                // file1.delete();
            }
        }


        log.info("{}", stringStringMap);
        if (stringStringMap != null && !StrUtil.isEmpty(stringStringMap.get("url").toString())) {
            // String url = sysFileService.getFileUrl(stringStringMap.get("fileName"));
            String fileUrl = sysFileService1.getFileUrl(stringStringMap.get("bucketName").toString(), stringStringMap.get("fileName").toString());
            log.info(fileUrl);
            set.set(User::getPortrait, fileUrl);
        } else {
            return R.error("Failed to upload portrait");
        }

        try {
            String address = transactionUtils.register(byId.getUserCode());
            if (address == null || address.length() < 1) {
                return R.error("Failed to register wallet");
            }
            set.set(User::getWallet, address);
        } catch (Exception e) {
            log.error("{}", e);
        }
        String emToken = "";
        try {
            EMUser emUser = emTemplate.createUser(byId.getUserCode(), ChatConfig.EM_PWD + byId.getUserCode());
            if (emUser == null || emUser.getUuid() == null) {
                return R.error("Failed to register em ");
            }
            set.set(User::getEmUuid, emUser.getUuid());
            emToken = emTemplate.getUserToken(byId.getUserCode(), emUser.getUuid(), 0, ChatConfig.EM_PWD + byId.getUserCode());
        } catch (Exception e) {
            log.error("{}", e);
        }
        set.set(User::getRegistered, 1);
        update(byId, set);
        User byId1 = getById(query.getUserCode());
        UserDto userDto = User2DtoMapper.INSTANCE.user2Dto(byId1);
        userDto.setFirstLogin(true);
        JSONObject entries = JSONUtil.parseObj(userDto);
        entries.put("emToken", emToken);
        TokenDto tokenDto = new TokenDto();
        tokenDto.setUserId(byId.getUserCode());
        tokenDto.setRegion(region);
        tokenDto.setIpaddr(ipv4);
        tokenDto.setLast(new Date());
        tokenDto.setUserType("member");
        tokenDto.setUserAccount(byId.getEmail());
        tokenDto.setExpireTime(TokenConstant.EXPIRE_TIME);
        Map<String, Object> token = tokenService.createToken(tokenDto);
        String invitationCode = byId.getInvitationCode();
        if (!StrUtil.isEmpty(invitationCode)) {
            boolean inv = invitationRewardsService.setInvitationRewards(ConsumerEnum.SPEND.getSign(), byId.getUserCode(), invitationCode);
            boolean inv1 = invitationRewardsService.setInvitationRewards(ConsumerEnum.EARN.getSign(), byId.getUserCode(), invitationCode);

        }

        //新增缓存
        userCache.AddOneUser(byId.getUserCode(), byId1);
        if (StrUtil.isNotEmpty(byId.getAddress())) {
            userCache.AddByAddress(byId.getAddress(), byId);
        }

        entries.putAll(token);
        boolean firstLogin = redisUtil.hHasKey(RedisKeys.USER + ":inv:l" + DateUtils.getSimpleToday(), byId.getUserCode());
        entries.set("sayHi", "[]");
        log.info("firstLogin:{}", firstLogin);
        if (!firstLogin) {
            List<HashMap<String, String>> customMsg = customMsgService.getCustomMsg(byId.getUserCode(), byId.getUserName());
            if (!CollUtil.isEmpty(customMsg)) {
                for (HashMap<String, String> map : customMsg) {
                    String reply = map.getOrDefault("reply", "");
                    String cuserCode = map.getOrDefault("cuserCode", "");
                    // myMsgDelayQueue.add(new MsgDelayTask(byId.getUserCode(), cuserCode, reply, 1000L * (RandomUtil.randomInt(10, 100))));
                }
            }
            entries.set("sayHi", JSONUtil.parseArray(customMsg));
        }
        invitationCodeTotalService.changeTotal(ActionEnum.SIGN_IN.getSign(), 1, byId.getUserCode(), 0);
        log.info(byId.getEmail() + "   " + entries);
        return R.success("login_success", entries);
    }

    /**
     * 去掉指定字符串的开头的指定字符
     *
     * @param stream 原始字符串
     * @param trim   要删除的字符串
     * @return
     */
    public static String StringStartTrim(String stream, String trim) {
        // null或者空字符串的时候不处理
        if (stream == null || stream.length() == 0 || trim == null || trim.length() == 0) {
            return stream;
        }
        // 要删除的字符串结束位置
        int end;
        // 正规表达式
        String regPattern = "[" + trim + "]*+";
        Pattern pattern = Pattern.compile(regPattern, Pattern.CASE_INSENSITIVE);
        // 去掉原始字符串开头位置的指定字符
        Matcher matcher = pattern.matcher(stream);
        if (matcher.lookingAt()) {
            end = matcher.end();
            stream = stream.substring(end);
        }
        // 返回处理后的字符串
        return stream;
    }


    @Override
    public R registerV1(UserWalletAuth newUser) {


        log.info("registerV1,{}", newUser);


        String account = StrUtil.isEmpty(newUser.getAccount()) ? "" : newUser.getAccount().trim().toLowerCase();
        if (StrUtil.isEmpty(account)) {
            return R.error("Sorry, your address is INVALID");

        }
        boolean userRegistered = aptosService.checkUser(account);


        //boolean userRegistered = aptosService.checkUser(account);
        List<User> user_check = this.list(Wrappers.<User>lambdaQuery().eq(User::getAddress, account).last("limit 1"));
        //同ip限制
        if (!StrUtil.isEmpty(newUser.getIpV4())) {
            if (userCache.addAndValidationFailedTimes("login" + newUser.getIpV4()) >= 88) {
                return R.error("Sorry, your verification failed more than " + ChatConfig.MAX_VALIDATION_FAILED_TIMES + " times today");
            }
        }
        //同地址限制
        if (RateLimiterCache.moreThanOnce(account + "login", 3)) {
            return R.error("Sorry, your Login too frequently");
        }
        Long t2 = 0L, t3;
        User user = new User();
        //新增加用户
        if (CollectionUtil.isEmpty(user_check)) {
//            String inv = StringStartTrim(invitationCode, "0");
//            try {
//                int i = Integer.parseInt(inv);
//                if (i == 0 || i > 5000) {
//                    throw new Exception("");
//                }
//            } catch (Exception e) {
//                return R.error("Sorry, your invitationCode is INVALID");
//            }
            String portrait = "default";
            String lestUserCode = userIdUtils.getNewUserCode("");
            user = new User();
            user.setUserName(account);
            user.setAddress(account);
            user.setPortrait(portrait);
            user.setUserCode(lestUserCode);
            user.setRegistered(0);
            user.setGender(0);
            user.setCreatedTime(LocalDateTime.now());
            // user.setInvitationCode(newUser.getInvitationCode());
            boolean save = save(user);
            if (!save) {
                return R.custom(ResultCode.FAILED.getCode(), ResultCode.FAILED.getMessage());
            }
            UserDto userDto = User2DtoMapper.INSTANCE.user2Dto(user);
            userDto.setFirstLogin(true);
            JSONObject entries = JSONUtil.parseObj(userDto);
            entries.set("chainRegistered", userRegistered);
            return R.success("login_success", entries);
        } else {

            user = user_check.get(0);
            Integer registered = user.getRegistered();

            //链上注册完毕
            if (1 == registered) {
                if (!StrUtil.isEmpty(user.getInvitationCode())) {
                    try {
                        JSONArray userAssets = aptosService.getUserAssets(user.getAddress());
                        if (userAssets != null && userAssets.size() > 0 && (userAssets.getLong(0) == 0 || userAssets.getLong(1) == 0)) {
                            aptosService.dat3SysUserInit(user.getInvitationCode(), user.getUserCode(), user.getAddress());
                        }
                    } catch (Exception e) {
                        log.info("login userAssets " + e.fillInStackTrace());
                    }
                }
                if (emTemplate.getUser(user.getUserCode()) == null) {
                    emTemplate.createUser(user.getUserCode(), ChatConfig.EM_PWD + user.getUserCode());
                    modifyUserInfoBase(user.getUserCode(), user.getUserName(), user.getPortrait());
                }
                String emToken = emTemplate.getUserToken(user.getUserCode(), user.getEmUuid(), 0, ChatConfig.EM_PWD + user.getUserCode());
                t3 = System.currentTimeMillis();
                log.info("t3-t2:{}", t3 - t2);
                UserDto userDto = User2DtoMapper.INSTANCE.user2Dto(user);
                userDto.setFirstLogin(false);
                JSONObject entries = JSONUtil.parseObj(userDto);
                entries.put("emToken", emToken);
                TokenDto tokenDto = new TokenDto();
                tokenDto.setUserId(user.getUserCode());
                tokenDto.setRegion(newUser.getRegions());
                tokenDto.setIpaddr(newUser.getIpV4());
                tokenDto.setLast(new Date());
                tokenDto.setUserType("member");
                tokenDto.setUserAccount(user.getEmail());
                tokenDto.setExpireTime(TokenConstant.EXPIRE_TIME);
                Map<String, Object> token = tokenService.createToken(tokenDto);
                //新增缓存
                userCache.AddOneUser(user.getUserCode(), user);
                if (StrUtil.isNotEmpty(user.getAddress())) {
                    userCache.AddByAddress(user.getAddress(), user);
                }
                entries.putAll(token);
                entries.set("sayHi", "[]");
                entries.set("chainRegistered", userRegistered);
                boolean firstLogin = redisUtil.hHasKey(RedisKeys.USER + ":inv:l" + DateUtils.getSimpleToday(), user.getUserCode());
                log.info("firstLogin:{}", firstLogin);
                if (!firstLogin) {
                    List<HashMap<String, String>> customMsg = customMsgService.getCustomMsg(user.getUserCode(), user.getUserName());
                    if (!CollUtil.isEmpty(customMsg)) {
                        for (HashMap<String, String> map : customMsg) {
                            String reply = map.getOrDefault("reply", "");
                            String cuserCode = map.getOrDefault("cuserCode", "");
                            //log.info("cuserCode:{},{},{}",firstLogin,user.getUserCode(),reply);
                            // myMsgDelayQueue.add(new MsgDelayTask(user.getUserCode(), cuserCode, reply, 1000L * (RandomUtil.randomInt(10, 100))));
                        }
                    }
                    entries.set("sayHi", JSONUtil.parseArray(customMsg));
                }

                userDto.setFirstLogin(false);
                entries.set("chainRegistered", userRegistered);
                return R.success("login_success", entries);

            }
            //注册第一步完毕


        }
        UserDto userDto = User2DtoMapper.INSTANCE.user2Dto(user);
        userDto.setFirstLogin(true);
        JSONObject entries = JSONUtil.parseObj(userDto);
        entries.set("chainRegistered", userRegistered);
        return R.success("login_success", entries);

    }

    @Override
    public R registerV1End(UserDto query, MultipartFile file, String ipv4, String region) {

        User byId = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUserCode, query.getUserCode())
                .eq(User::getRegistered, 0));
        if (byId == null) {
            return R.error("Failed to register");
        }
        LambdaUpdateWrapper<User> set = new LambdaUpdateWrapper<User>()
                .eq(User::getUserCode, query.getUserCode());
        if (!StrUtil.isEmpty(query.getUserName())) {
            set.set(User::getUserName, query.getUserName());
        }
        if (!checkUserName(query.getUserName())) {
            return R.error("your nickname is not available");
        }
        if (!StrUtil.isEmpty(query.getBio())) {
            set.set(User::getBio, query.getBio());
        }
        if (query.getGender() != null && query.getGender() >= 0 && query.getGender() < 4) {
            set.set(User::getGender, query.getGender());
        } else {
            set.set(User::getGender, 0);
        }

        if (file == null || file.isEmpty()) {
            return R.error("Failed to upload portrait");
        }
        String portrait = byId.getPortrait();
        long size = file.getSize();
        String tempFileName = ChatConfig.RESOURCES_PATH + System.currentTimeMillis() + file.getOriginalFilename();
        Map<String, Object> stringStringMap = null;
        if (size < 254694) {
            stringStringMap = sysFileService.uploadFile(file, query.getUserCode(), 0);

        } else {
            try {

                boolean write = Img.from(file.getInputStream()).setQuality(0.8F).write(new File(tempFileName));
                if (write) {
                    stringStringMap = sysFileService.uploadFile(Files.newInputStream(Paths.get(tempFileName)), file.getOriginalFilename(), file.getName(), file.getContentType(), query.getUserCode());
                }
            } catch (Exception e) {
                e.fillInStackTrace();
            } finally {
                File file1 = new File(tempFileName);
                file1.setWritable(true, false);
                // file1.delete();
            }
        }

        String fileUrl = "default";
        log.info("{}", stringStringMap);
        if (stringStringMap != null && !StringUtil.isEmpty(stringStringMap.get("url").toString())) {
            // String url = sysFileService.getFileUrl(stringStringMap.get("fileName"));
            fileUrl = sysFileService1.getFileUrl(stringStringMap.get("bucketName").toString(), stringStringMap.get("fileName").toString());
            log.info(fileUrl);
            set.set(User::getPortrait, fileUrl);


        } else {
            return R.error("Failed to upload portrait");
        }

        try {
            String address = transactionUtils.register(byId.getUserCode());
            if (address == null || address.length() < 1) {
                return R.error("Failed to register wallet");
            }
            set.set(User::getWallet, address);
        } catch (Exception e) {
            log.error("{}", e);
        }
        String emToken = "";
        try {
            EMUser emUser = emTemplate.createUser(byId.getUserCode(), ChatConfig.EM_PWD + byId.getUserCode());
            if (emUser == null || emUser.getUuid() == null) {
                return R.error("Failed to register em ");
            }
            set.set(User::getEmUuid, emUser.getUuid());
            HashMap map = new HashMap();

            map.put("nickname", query.getUserName());
            map.put("avatarurl", fileUrl);
            emTemplate.setMetadataToUser(byId.getUserCode(), map);
            emToken = emTemplate.getUserToken(byId.getUserCode(), emUser.getUuid(), 0, ChatConfig.EM_PWD + byId.getUserCode());
        } catch (Exception e) {
            log.error("{}", e);
        }
        set.set(User::getRegistered, 1);
        update(byId, set);
        User byId1 = getById(query.getUserCode());
        UserDto userDto = User2DtoMapper.INSTANCE.user2Dto(byId1);
        userDto.setFirstLogin(true);
        JSONObject entries = JSONUtil.parseObj(userDto);
        entries.put("emToken", emToken);
        TokenDto tokenDto = new TokenDto();
        tokenDto.setUserId(byId.getUserCode());
        tokenDto.setRegion(region);
        tokenDto.setIpaddr(ipv4);
        tokenDto.setLast(new Date());
        tokenDto.setUserType("member");
        tokenDto.setUserAccount(byId.getEmail());
        tokenDto.setExpireTime(TokenConstant.EXPIRE_TIME);
        Map<String, Object> token = tokenService.createToken(tokenDto);
        String invitationCode = byId.getInvitationCode();


        //新增缓存
        userCache.AddOneUser(byId.getUserCode(), byId);
        if (StrUtil.isNotEmpty(byId.getAddress())) {
            userCache.AddByAddress(byId.getAddress(), byId);
        }
        entries.putAll(token);
        boolean firstLogin = redisUtil.hHasKey(RedisKeys.USER + ":inv:l" + DateUtils.getSimpleToday(), byId.getUserCode());
        entries.set("sayHi", "[]");
        log.info("firstLogin:{}", firstLogin);
        if (!firstLogin) {
            List<HashMap<String, String>> customMsg = customMsgService.getCustomMsg(byId.getUserCode(), byId.getUserName());
            if (!CollUtil.isEmpty(customMsg)) {
                for (HashMap<String, String> map : customMsg) {
                    String reply = map.getOrDefault("reply", "");
                    String cuserCode = map.getOrDefault("cuserCode", "");
                    //  myMsgDelayQueue.add(new MsgDelayTask(byId.getUserCode(), cuserCode, reply, 1000L * (RandomUtil.randomInt(10, 100))));
                }
            }
            entries.set("sayHi", JSONUtil.parseArray(customMsg));
        }
        log.info(byId.getEmail() + "   " + entries);
        return R.success("login_success", entries);
    }

    @Override
    public boolean checkUserName(String userName) {
        if (StrUtil.isEmpty(userName)) {
            return false;
        }
        Map<String, User> allUser = userCache.getAllUser();
        boolean b = allUser.values().stream().noneMatch(s -> userName.equals(s.getUserName()));
        return b;
    }


    @Override
    public R login(UserAuthDto newUser) {
        CaptchaDto captchaDto = new CaptchaDto();
        captchaDto.setAccount(newUser.getAccount());
        //获取验证码类型
        String captchaType = CaptchaTypeEnum.LOGIN.getSign();
        captchaDto.setType(captchaType);
        //验证失败次数大于设定阈值
        if (userCache.validationFailed("login" + newUser.getAccount()) >= ChatConfig.MAX_VALIDATION_FAILED_TIMES) {
            return R.error("Sorry, your verification failed more than " + ChatConfig.MAX_VALIDATION_FAILED_TIMES + " times today");
        }
        // 从Redis中获取缓存验证码
        CaptchaDto captcha = tokenService.parseCaptcha(captchaDto);
        //进行验证码的比对（页面提交的验证码和redis中保存的验证码比对）
        if (captcha == null
                || captcha.getCaptcha() == null
                || !captcha.getCaptcha().equals(newUser.getCaptcha())) {
            //验证失败次数+1
            userCache.addValidationFailed("login" + newUser.getAccount());
            return R.custom(ResultCode.FORBIDDEN.getCode(), "The verification code is incorrect or expired");
        }


        List<User> list = list(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, newUser.getAccount())
                .or()
                .eq(User::getEmail, newUser.getAccount()).last("limit 1"));
        if (CollectionUtil.isEmpty(list)) {
            return R.error("no such user:" + newUser.getAccount());
        }

        User user = list.get(0);

        HMac mac = new HMac(HmacAlgorithm.HmacSHA1, newUser.getAccount().getBytes());
        if (!mac.verify(mac.digestHex(user.getPassword().getBytes()).getBytes(), newUser.getPassword().getBytes())) {
            return R.error("Password error:" + newUser.getAccount());
        }

        String emToken = emTemplate.getUserToken(user.getUserCode(), user.getEmUuid(), 0, ChatConfig.EM_PWD + user.getUserCode());
        UserDto userDto = User2DtoMapper.INSTANCE.user2Dto(user);
        JSONObject entries = JSONUtil.parseObj(userDto);
        entries.put("emToken", emToken);
        TokenDto tokenDto = new TokenDto();
        tokenDto.setUserAccount(user.getEmail());
        tokenDto.setAddress(user.getWallet());
        tokenDto.setUserType("member");
        tokenDto.setUserId(user.getUserCode());
        tokenDto.setIpaddr(newUser.getIpV4());
        tokenDto.setRegion(newUser.getRegions());
        tokenDto.setLast(new Date());
        tokenDto.setExpireTime(SecurityConstant.TOKEN_EXPIRE_TIME);
        Map<String, Object> token = tokenService.createToken(tokenDto);
        entries.putAll(token);
        return R.success(entries);
    }

    @Override
    public UserDto getUserInfo(UserDto userDto, Boolean re) {

        LambdaQueryWrapper<User> query = Wrappers.<User>lambdaQuery();
        if (!StrUtil.isEmpty(userDto.getUserCode())) {
            query.eq(User::getUserCode, userDto.getUserCode());
        } else if (!StrUtil.isEmpty(userDto.getEmail())) {
            query.eq(User::getEmail, userDto.getEmail());

        } else if (!StrUtil.isEmpty(userDto.getWallet())) {
            query.eq(User::getWallet, userDto.getWallet());
        }
        if (re) {
            query.eq(User::getRegistered, 1).last("limit 1");
        }
        List<User> list = this.list(query);
        if (CollectionUtil.isEmpty(list)) {
            return null;
        }
        User user = list.get(0);

        UserDto userDto1 = User2DtoMapper.INSTANCE.user2Dto(user);
        JSONObject entries = JSONUtil.parseObj(userDto1);
        Creator creator = creatorService.getById(user.getUserCode());
        entries.set("show", true);
        entries.set("profession", "[]");
        entries.set("online", userCache.userOnlineGet(user.getUserCode()));
        if (creator != null) {
            entries.set("show", true);
            entries.set("profession", creator.getProfession());
            if (creator.getOnline() != null && creator.getOnline() == 1) {
                entries.set("online", System.currentTimeMillis());
            }

        }
        return userDto1;
    }

    @Override
    public R getUser(UserDto userDto) {

        List<User> list = new ArrayList<>();
        if (!StrUtil.isEmpty(userDto.getAddress())) {
            list = list(Wrappers.<User>lambdaQuery()
                    .eq(User::getAddress, userDto.getAddress()).last("limit 1"));
        }
        if (!StrUtil.isEmpty(userDto.getUserCode())) {
            list = list(Wrappers.<User>lambdaQuery()
                    .eq(User::getUserCode, userDto.getUserCode()).last("limit 1"));
        } else if (!StrUtil.isEmpty(userDto.getEmail())) {
            list = list(Wrappers.<User>lambdaQuery()
                    .eq(User::getEmail, userDto.getEmail()).last("limit 1"));

        } else if (!StrUtil.isEmpty(userDto.getWallet())) {
            list = list(Wrappers.<User>lambdaQuery()
                    .eq(User::getWallet, userDto.getWallet()).last("limit 1"));

        }
        if (CollectionUtil.isEmpty(list)) {
            return null;
        }
        User user = list.get(0);

        UserDto userDto1 = User2DtoMapper.INSTANCE.user2Dto(user);
        JSONObject entries = JSONUtil.parseObj(userDto1);
        LinkedList<Map<String, Object>> filesUrl = sysFileService1.getFilesUrl(user.getUserCode());
        entries.set("files",filesUrl);
        Creator creator = creatorService.getById(user.getUserCode());
        entries.set("show", false);
        entries.set("profession", "[]");
        //todo 扩展字段
        entries.set("professionBio", user.getBio());
        entries.set("online", userCache.userOnlineGet(user.getUserCode()));
        if (creator != null) {
            entries.set("show", true);
            entries.set("profession", creator.getProfession());
            if (creator.getOnline() != null && creator.getOnline() == 1) {
                entries.set("online", System.currentTimeMillis());
            }

        }
        return R.success(entries);
    }

    @Override
    public R modifyUserInfo(UserDto query, MultipartFile file) {

        User byId = getById(query.getUserCode());
        LambdaUpdateWrapper<User> set = new LambdaUpdateWrapper<User>()
                .eq(User::getUserCode, query.getUserCode());

        if (!StrUtil.isEmpty(query.getUserName())) {
            set.set(User::getUserName, query.getUserName());
            HashMap map = new HashMap();
            map.put("nickname", query.getUserName());
            emTemplate.setMetadataToUser(query.getUserCode(), map);
        }
        if (!StrUtil.isEmpty(query.getBio())) {
            set.set(User::getBio, query.getBio());
        }
        if (query.getGender() != null && query.getGender() >= 0 && query.getGender() < 4) {
            set.set(User::getGender, query.getGender());
        } else {
            set.set(User::getGender, 0);
        }

        if (file != null && !file.isEmpty()) {
            String portrait = byId.getPortrait();
            if (portrait != null && !"default".equals(portrait)) {
                String[] split = portrait.split("/");
                sysFileService.deleteFile(null, split[split.length - 1]);
            }
            long size = file.getSize();
            Map<String, Object> stringStringMap = sysFileService.uploadFile(file, query.getUserCode(), 0);
            log.info("{}", stringStringMap);
            if (stringStringMap != null && !StringUtil.isEmpty("" + stringStringMap.get("url"))) {
                // String url = sysFileService.getFileUrl(stringStringMap.get("fileName"));
                String fileUrl = sysFileService1.getFileUrl("" + stringStringMap.get("bucketName"), "" + stringStringMap.get("fileName"));
                log.info(fileUrl);
                set.set(User::getPortrait, fileUrl);
            } else {
                return R.error("Failed to upload portrait");
            }
        }

        userCache.AddOneUser(byId.getUserCode(), byId);
        if (StrUtil.isNotEmpty(byId.getAddress())) {
            userCache.AddByAddress(byId.getAddress(), byId);
        }
        update(byId, set);
        User byId1 = getById(query.getUserCode());
        UserDto userDto = User2DtoMapper.INSTANCE.user2Dto(byId1);
        return R.success(userDto);
    }

    @Override
    public R modifyUserPortrait(String userCode, MultipartFile file) {
        User byId = getById(userCode);
        LambdaUpdateWrapper<User> set = new LambdaUpdateWrapper<User>()
                .eq(User::getUserCode, userCode);
        if (file != null && !file.isEmpty()) {
            String portrait = byId.getPortrait();
            if (portrait != null && !"default".equals(portrait)) {
                String[] split = portrait.split("/");
                try {
                    sysFileService.deleteFile(null, split[split.length - 1]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            long size = file.getSize();
            Map<String, Object> stringStringMap = sysFileService.uploadFile(file, userCode, 0);
            log.info("{}", stringStringMap);
            if (stringStringMap != null && !StringUtil.isEmpty("" + stringStringMap.get("url"))) {
                // String url = sysFileService.getFileUrl(stringStringMap.get("fileName"));
                String fileUrl = sysFileService1.getFileUrl("" + stringStringMap.get("bucketName"), "" + stringStringMap.get("fileName"));
                log.info(fileUrl);
                byId.setPortrait(fileUrl);
                set.set(User::getPortrait, fileUrl);
                User user = new User();
                user.setUserCode(userCode);
                user.setPortrait(fileUrl);
                update(user, set);
                UserDto userDto = User2DtoMapper.INSTANCE.user2Dto(byId);

                userCache.AddOneUser(byId.getUserCode(), byId);
                if (StrUtil.isNotEmpty(byId.getAddress())) {
                    userCache.AddByAddress(byId.getAddress(), byId);
                }
                return R.success(userDto);
            }


        }

        return R.error("Failed to upload portrait");

    }

    @Override
    public Boolean modifyUserInfoBase(String userCode, String name, String file) {

        if (StrUtil.isNotEmpty(name) || StrUtil.isNotEmpty(file)) {
            User user = new User();
            user.setUserCode(userCode);
            if (StrUtil.isNotEmpty(name)) {
                user.setUserName(name);
            }
            if (StrUtil.isNotEmpty(file)) {
                user.setPortrait(file);
            }
            boolean b = this.updateById(user);
            if (b) {
                if (StrUtil.isNotEmpty(name) && StrUtil.isNotEmpty(file)) {

                    User user1 = userCache.getUser(userCode);
                    user1.setUserName(name);
                    user1.setPortrait(file);
                    userCache.AddByAddress(user1.getAddress(), user1);
                    HashMap map = new HashMap();
                    map.put("nickname", name);
                    map.put("avatarurl", file);
                    emTemplate.setMetadataToUser(user1.getUserCode(),map);
                }
                if (StrUtil.isNotEmpty(name)) {
                    User user1 = userCache.getUser(userCode);

                    user1.setUserName(name);
                    userCache.AddByAddress(user1.getAddress(), user1);

                    HashMap map = new HashMap();
                    map.put("nickname", name);
                    emTemplate.setMetadataToUser(user1.getUserCode(),map);
                }
                if (StrUtil.isNotEmpty(file)) {
                    User user1 = userCache.getUser(userCode);
                    user1.setPortrait(file);
                    userCache.AddByAddress(user1.getAddress(), user1);

                    HashMap map = new HashMap();
                    map.put("avatarurl", file);
                    emTemplate.setMetadataToUser(user1.getUserCode(),map);
                }

            }
            return b;
        }
        return false;

    }

    @Override
    public R logout(String email) {
        try {
            String tokenKey = "sso:token:member:" + email;
            Object obj = redisUtil.get(tokenKey);
            //使当前token失效,记录上次登陆时间
            if (obj != null) {
                long expire = redisUtil.getExpire(tokenKey);
                TokenDto old = (TokenDto) obj;
                old.setExpireTime(expire);
                tokenService.createToken(old);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.success("Your account has been safely logout");
    }


}
