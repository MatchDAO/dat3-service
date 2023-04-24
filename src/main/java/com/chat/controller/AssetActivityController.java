package com.chat.controller;

import cn.hutool.core.img.Img;
import cn.hutool.core.util.StrUtil;
import com.chat.common.*;
import com.chat.config.ChatConfig;
import com.chat.entity.AssetActivity;
import com.chat.entity.User;
import com.chat.entity.dto.UserDto;
import com.chat.entity.dto.WalletAssetActivity;
import com.chat.entity.dto.WalletAssetActivityResult;
import com.chat.service.SysFileService;
import com.chat.service.TransactionUtils;
import com.chat.service.impl.AssetActivityServiceImpl;
import com.chat.service.impl.SysFileServiceImpl;
import com.chat.service.impl.UserServiceImpl;
import com.chat.utils.JwtUtils;
import com.chat.utils.MessageUtils;
import com.chat.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 资产变动
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
@Slf4j
@RestController
@AuthToken
@RequestMapping("/assetActivity")
public class AssetActivityController {

    @Resource
    private AssetActivityServiceImpl activityService;
    @Resource
    private SysFileService sysFileService;
    @Resource
    private SysFileServiceImpl sysFileService1;
    @Resource
    private TransactionUtils transactionUtils;
    @Resource
    private UserServiceImpl userService;

    @AuthToken(validate = false)
    @RequestMapping("/test")
    public R test1(UserDto query ,@RequestParam(value = "file", required = false) MultipartFile file) {
        String tempFileName = ChatConfig.RESOURCES_PATH + System.currentTimeMillis()+file.getOriginalFilename();
        Map<String, Object> stringStringMap = null;
        try {

            boolean write = Img.from(file.getInputStream()).setQuality(0.8F).write(new File(tempFileName));
            if (write) {
                stringStringMap = sysFileService.uploadFile(Files.newInputStream(Paths.get(tempFileName)), file.getOriginalFilename(), file.getName(), file.getContentType(), query.getUserCode());
            }
        } catch (Exception e) {
            e.fillInStackTrace();
        }finally {
            File file1 = new File(tempFileName);
            file1.setWritable(true, false);
            file1.delete();
        }
        log.info("{}", stringStringMap);
        if (stringStringMap != null && !StringUtil.isEmpty(stringStringMap.get("url").toString())) {
            // String url = sysFileService.getFileUrl(stringStringMap.get("fileName"));
            String fileUrl = sysFileService1.getFileUrl(stringStringMap.get("bucketName").toString(), stringStringMap.get("fileName").toString());

            log.info(fileUrl);

        } else {
            return R.error(MessageUtils.getLocale("failed.upload") );
        }

        LocalDateTime now = LocalDateTime.now();
        // 获取秒数 gmt+8
        Long second8 = now.toEpochSecond(ZoneOffset.of("+8"));
        // 获取毫秒数 gmt+8
        Long milliSecond8 = now.toInstant(ZoneOffset.of("+8")).toEpochMilli();

        // 获取秒数 gmt+0
        Long second0 = now.toEpochSecond(ZoneOffset.of("+0"));
        // 获取毫秒数 gmt+0
        Long milliSecond0 = now.toInstant(ZoneOffset.of("+0")).toEpochMilli();

        // 获取秒数 currentTimeMillis
        long totalMilliSeconds = System.currentTimeMillis();

        TimeZone timeZone = TimeZone.getDefault();
        System.out.println("System Default TimeZone: " + timeZone.getID());

        System.out.println("now:            " + now);
        System.out.println("+8 second:      " + second8);
        System.out.println("+8 milliSecond: " + milliSecond8);

        System.out.println("+0 seconds:     " + second0);
        System.out.println("+0 milliSeconds:" + milliSecond0);

        // 显示时间
        System.out.println("+s seconds:     " + totalMilliSeconds);
        TimeZone aDefault = TimeZone.getDefault();
        System.out.println(System.currentTimeMillis());
        log.info("{},{}", aDefault, System.currentTimeMillis());
        return activityService.test();
    }

    @GetMapping("/modifyState")
    @AuthToken(validate = false)
    public R modifyState(@RequestParam String transactionOrderId
            , @RequestParam String confirmState
            , @RequestParam(required = false) String txnHash, HttpServletRequest request) {

        if (StrUtil.isEmpty(transactionOrderId)) {
            log.error("transactionOrderId:{}", transactionOrderId);
            return R.error("transactionOrderId:" + transactionOrderId);
        }
        if (confirmState == null) {
            log.error("confirmState:{}", confirmState);
            return R.error("confirmState:" + confirmState);
        }

        AssetActivity byId = activityService.getById(transactionOrderId);
        if (byId == null) {
            log.error("activity is null :" + transactionOrderId);
            return R.error("activity is null :" + transactionOrderId);
        }
        AssetActivity build = AssetActivity.builder()
                .id(transactionOrderId)
                .confirmState(ConfirmStateEnum.of(confirmState).getType())
                .build();
        if (StrUtil.isEmpty(txnHash)) {
            log.error("txnHash:{}", txnHash);
        } else {
            build.setTransactionHash(txnHash);
        }
        boolean b = activityService.updateById(build);
        return R.success(b);
    }

    @GetMapping("/chat/rewards")
    @AuthToken(validate = false)
    public R rewards(@RequestHeader(value = "token") String token){
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
        User byId = userService.getById(userCode);
        HashMap<String, Object> res = new HashMap<>();
        res.put("rewardETH","0");
        res.put("rewardDAT3","0");
        res.put("rewardDAT3Record",new ArrayList<>());
        res.put("DAT3USDT","0");
        try {
            WalletAssetActivityResult rewardETH = transactionUtils.activity("", byId.getWallet(), ActivityType.REPLY_MSG.getName());
            if (rewardETH!=null&&rewardETH.getCode()==200) {
                List<WalletAssetActivity> data = rewardETH.getData();
                LongSummaryStatistics rewardETHs = data.stream()
                        .filter(s -> !StrUtil.isEmpty(s.getValue()))
                        .collect(Collectors.summarizingLong(s -> new BigInteger(s.getValue()).longValue()));
                long sumETH = rewardETHs.getSum();
                res.put("rewardETH",""+sumETH);

            }
            WalletAssetActivityResult rewardDAT3 = transactionUtils.activity("", byId.getWallet(), ActivityType.REWARD_DAT3.getName());
            if (rewardDAT3!=null&&rewardDAT3.getCode()==200) {
                List<WalletAssetActivity> data3 = rewardDAT3.getData();
                LongSummaryStatistics rewardDAT3s = data3.stream()
                        .filter(s -> !StrUtil.isEmpty(s.getValue()))
                        .collect(Collectors.summarizingLong(s -> new BigInteger(s.getValue()).longValue()));
                long sumDat3 = rewardDAT3s.getSum();
                res.put("rewardDAT3",""+sumDat3);
                res.put("rewardDAT3Record",data3);
            }
        } catch (Exception e) {
            log.error(""+e);
           e.fillInStackTrace();
        }
        return R.success(res);
    }
}

