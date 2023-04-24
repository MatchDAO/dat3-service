package com.chat.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.chat.common.*;
import com.chat.config.own.PrivateConfig;
import com.chat.entity.AssetActivity;
import com.chat.entity.User;
import com.chat.entity.dto.UserDto;
import com.chat.entity.dto.WalletBalance;
import com.chat.entity.dto.WalletTicker;
import com.chat.entity.dto.WalletUserResult;
import com.chat.service.TransactionUtils;
import com.chat.service.impl.AssetActivityServiceImpl;
import com.chat.service.impl.UserServiceImpl;
import com.chat.utils.Coder;
import com.chat.utils.DESCoder;
import com.chat.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.WalletUtils;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
@RequestMapping("/wallet")
@AuthToken
public class WalletController {

    @Resource
    private UserServiceImpl userService;
    @Resource
    private AssetActivityServiceImpl activityService;
    @Resource
    private TransactionUtils transactionUtils;


    @PostMapping("/getDepositAddress")
    public R getDepositAddress(@RequestBody Map<String, String> request) throws Exception {

        String userId1 = request.get("userCode");
        if (StrUtil.isEmpty(userId1)) {
            return R.error("userId is null");
        }
        User byId = userService.getById(userId1);
        if (byId == null) {
            return R.error("No information about you");
        }
        WalletUserResult userDeposit = transactionUtils.getUserDeposit(userId1);
        if (userDeposit == null) {
            return R.error("No information about your wallet");
        }
        return R.success(userDeposit.getAddress());
    }

    @PostMapping("/balance")
    public R balance(@RequestBody Map<String, String> request) throws Exception {

        String wallet = request.get("wallet");
        if (StrUtil.isEmpty(wallet)) {
            return R.error("wallet is null");
        }
        UserDto byId = userService.getUserInfo(UserDto.builder().wallet(wallet).build(),true);
        if (byId == null) {
            return R.error("No information about you");
        }
        List<WalletBalance> walletBalances = transactionUtils.balance(wallet);
        if (CollUtil.isEmpty(walletBalances)) {
            return R.error("No information about your wallet");
        }
        return R.success(walletBalances);
    }

    @PostMapping("/exchangePrice")
    public R exchangePrice(@RequestBody WalletTicker request) throws Exception {
        if (request == null || StrUtil.isEmpty(request.getSymbol1()) || StrUtil.isEmpty(request.getSymbol2())) {
            return R.error("Ticker is null");
        }
        WalletTicker ticker = transactionUtils.exchangePrice(request.getSymbol1(), request.getSymbol2());
        return R.success(ticker);
    }

    @PostMapping("/withdraw")
    public R withdrawAsset(@RequestBody HashMap<String, String> request, @RequestHeader(value = "token") String assToken) throws Exception {

        String u1 = null;
        try {
            u1 = JwtUtils.getUserKey(assToken);
            if (u1 == null) {
                return R.error("account is error");
            }
        } catch (Exception e) {
            log.error("`Invalid user" + e.fillInStackTrace());
            return R.error("`Invalid user information:" + assToken);
        }
        UserDto user = userService.getUserInfo(UserDto.builder().userCode(u1).build(),true);
        if (request == null || CollUtil.isEmpty(request)) {
            return R.error("withdraw is error");
        }
        String transferAddress = request.get("transferAddress");
        String token = request.get("token");
        String chain = request.get("chain");
        String amount = request.get("amount");
        try {
            if (!WalletUtils.isValidAddress(transferAddress)) {
                return R.error("`Invalid account:" + transferAddress);
            }
        } catch (Exception e) {
            log.error("{},{}", transferAddress,e);
            return R.error("`Invalid account");
        }
        TokenEnum ofToken = TokenEnum.of(token);
        if (ofToken == null) {
            return R.error("`Invalid token:" + token);
        }
        ChainEnum ofChain = ChainEnum.of(chain);
        if (!StrUtil.isNumeric(amount)) {
            return R.error("`Invalid amount:" + amount);
        }
        String password = URLEncoder.encode(Coder.encryptBASE64(DESCoder.encrypt(user.getWallet().getBytes(), PrivateConfig.TR_KEY))
                .replace("\n", ""));
        String transactionOrderId = "" + System.currentTimeMillis() + user.getUserCode();
        List<WalletBalance> balance = transactionUtils.balance(user.getWallet());
        List<WalletBalance> collect = balance.stream().filter(s -> s.getToken().equalsIgnoreCase(ofToken.getSymbol())).collect(Collectors.toList());
        if (CollUtil.isEmpty(collect)) {
            return R.error("error Balance:" + amount);
        }
        WalletBalance balance1 = collect.get(0);
        if (new BigInteger(balance1.getAmount()).subtract(new BigInteger(amount)).signum() < 0) {
            return R.custom(ResultCode.FROZEN.getCode(), "Insufficient amount");
        }
        WalletUserResult walletUserResult = transactionUtils.withdrawAsset(password, user.getWallet(), transferAddress, ofToken.getSymbol(), ofChain.getName(), amount, transactionOrderId);
        if (walletUserResult != null && walletUserResult.getCode() == 200) {
            AssetActivity build = AssetActivity.builder()
                    .id(transactionOrderId)
                    .fromAddress(user.getWallet())
                    .toAddress(transferAddress)
                    .amount(amount)
                    .token(ofToken.getSymbol())
                    .chain(ofChain.getName())
                    .type(ActivityType.WITHDRAW.getName())
                    .createTime(LocalDateTime.now())
                    .confirmState(ConfirmStateEnum.APPROVING.getType())
                    .build();
            boolean save = activityService.save(build);
            if (save) {
                return R.success(build);
            }
            return R.custom(ResultCode.FAILED.getCode(), ResultCode.FAILED.getMessage());
        }

        return R.custom(ResultCode.FAILED.getCode(), (walletUserResult == null||StrUtil.isEmpty(walletUserResult.getMessage())) ? ResultCode.FAILED.getMessage() : walletUserResult.getMessage());
    }

}
