package com.chat.service;

import cn.hutool.json.JSONUtil;
import com.chat.cache.TickerBase;
import com.chat.common.ResultCode;
import com.chat.config.TransactionConfig;
import com.chat.entity.dto.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TransactionUtils {

    @Resource
    private TransactionConfig transactionConfig;
    private final static OkHttpClient httpClient = new OkHttpClient();

    public String register(String userId) throws Exception {
        Request request = new Request.Builder()
                .url(transactionConfig.getTransactionUrl() + "/user/register/" + userId)
                .get()
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();
        log.info("Wallet register:{}-----------resp:{}", respStr, resp);
        WalletUserResult result = JSONUtil.toBean(respStr, WalletUserResult.class);
        if (ResultCode.SUCCESS.getCode() == result.getCode()) {
            return result.getAddress().get(0).getAddress();
        }
        return null;
    }

    public Boolean unlock(String password, String userAddress, String lock) throws Exception {
        Request request = new Request.Builder()
                .url(transactionConfig.getTransactionUrl() + "/user/unlock"
                        + "?password=" + password
                        + "&userAddress=" + userAddress
                        + "&lock=" + lock)
                .get()
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();
        log.info("Wallet unlock:{}-----------resp:{}", respStr, resp);
        WalletUserResult result = JSONUtil.toBean(respStr, WalletUserResult.class);
        if (ResultCode.SUCCESS.getCode() == result.getCode()) {
            return true;
        }
        return false;
    }

    public WalletUserResult getUserDeposit(String userId) throws Exception {
        Request request = new Request.Builder()
                .url(transactionConfig.getTransactionUrl() + "/user/getUserDeposit/" + userId)
                .get()
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();
        log.info("Wallet getUserDeposit:{}-----------resp:{}", respStr, resp);
        WalletUserResult result = JSONUtil.toBean(respStr, WalletUserResult.class);
        if (ResultCode.SUCCESS.getCode() == result.getCode()) {
            return result;
        }
        log.info("{},{}", request.url().url().toString(), respStr);
        return null;
    }

    public List<WalletBalance> balance(String wallet) throws Exception {
        Request request = new Request.Builder()
                .url(transactionConfig.getTransactionUrl() + "/user/balance/" + wallet)
                .get()
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();
        log.info("Wallet balance:{}-----------resp:{}", respStr, resp);
        WalletBalanceResult result = JSONUtil.toBean(respStr, WalletBalanceResult.class);
        if (ResultCode.SUCCESS.getCode() == result.getCode()) {
            return result.getBalances();
        }
        log.info("{},{}", request.url().url().toString(), respStr);
        return new ArrayList<>();
    }

    //Already implemented in this service
    public WalletTicker exchangePrice(String sourceType, String toType) throws Exception {
        return TickerBase.currentSymbol(sourceType, toType);
    }

    public WalletUserResult exchange(String password, String publicAddress, String sourceType, String toType, String amount, String price) throws Exception {
        Request request = new Request.Builder()
                .url(transactionConfig.getTransactionUrl() + "/user/exchange"
                        + "?password=" + password
                        + "&publicAddress=" + publicAddress
                        + "&sourceType=" + sourceType
                        + "&toType=" + toType
                        + "&amount=" + amount
                        + "&price=" + price)
                .get()
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();
        log.info("Wallet exchange:{}-----------resp:{}", respStr, resp);
        WalletUserResult result = JSONUtil.toBean(respStr, WalletUserResult.class);
        return result;
    }

    public WalletUserResult withdrawAsset(String password, String userAddress, String transferAddress, String token, String chain, String amount, String transactionOrderId) throws Exception {
        Request request = new Request.Builder()
                .url(transactionConfig.getTransactionUrl() + "/user/withdraw/asset"
                        + "?password=" + password
                        + "&userAddress=" + userAddress
                        + "&transferAddress=" + transferAddress
                        + "&token=" + token
                        + "&chain=" + chain
                        + "&amount=" + amount
                        + "&transactionOrderId=" + transactionOrderId)
                .get()
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();
        log.info("Wallet withdrawAsset:{}-----------resp:{}", respStr, resp);
        WalletUserResult result = JSONUtil.toBean(respStr, WalletUserResult.class);
        return result;
    }

    public WalletUserResult transferAsset(String fromUserAddress,
                                          String toUserAddress,
                                          String type,
                                          String amount, String chain, String password, Integer flag) throws Exception {
        Request request = new Request.Builder()
                .url(transactionConfig.getTransactionUrl() + "/withdraw/asset"
                        + "?fromUserAddress=" + fromUserAddress
                        + "&toUserAddress=" + toUserAddress
                        + "&type=" + type
                        + "&amount=" + amount
                        + "&chain=" + chain
                        + "&password=" + password
                        + "&flag=" + flag)
                .get()
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();
        log.info("Wallet transferAsset:{}-----------resp:{}", respStr, resp);
        WalletUserResult result = JSONUtil.toBean(respStr, WalletUserResult.class);
        return result;
    }

    public WalletUserResult sendMsg(String fromUserAddress, String to,
                                    String token,
                                    String amount, String password, String transactionOrderId) throws Exception {
        Request request = new Request.Builder()
                .url(transactionConfig.getTransactionUrl() + "/chat/sendMsg"
                        + "?fromUserAddress=" + fromUserAddress
                        + "&to=" + to
                        + "&token=" + token
                        + "&amount=" + amount
                        + "&password=" + password
                        + "&transactionOrderId=" + transactionOrderId)
                .get()
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();
        log.info("Wallet sendMsg:{}-----------resp:{}", respStr, resp);
        WalletUserResult result = JSONUtil.toBean(respStr, WalletUserResult.class);
        return result;
    }

    public WalletUserResult reply(String fromUserAddress
            , String toUserAddress
            , String token
            , String amount
            , Integer sentCount
            , String password, String transactionOrderId) throws Exception {
        Request request = new Request.Builder()
                .url(transactionConfig.getTransactionUrl() + "/chat/reply"
                        + "?fromUserAddress=" + fromUserAddress
                        + "&toUserAddress=" + toUserAddress
                        + "&amount=" + amount
                        + "&token=" + token
                        + "&sentCount=" + sentCount
                        + "&password=" + password
                        + "&transactionOrderId=" + transactionOrderId)
                .get()
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();

        log.info("Wallet reply:{}", respStr);
        WalletUserResult result = JSONUtil.toBean(respStr, WalletUserResult.class);
        return result;
    }

    public WalletUserResult rtcFrozen(String fromUserAddress
            , String to
            , String amount
            , String password
            , String cMillis) throws Exception {
        Request request = new Request.Builder()
                .url(transactionConfig.getTransactionUrl() + "/rtc/frozen"
                        + "?fromUserAddress=" + fromUserAddress
                        + "&to=" + to
                        + "&amount=" + amount
                        + "&password=" + password
                        + "&cMillis=" + cMillis)
                .get()
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();

        log.info("Wallet rtcFrozen:{}-------Response:{}", respStr,resp);
        WalletUserResult result = JSONUtil.toBean(respStr, WalletUserResult.class);
        return result;
    }

    public WalletUserResult rtcPayment(String fromUserAddress
            , String to
            , String password
            , String bTime, String channel) throws Exception {
        Request request = new Request.Builder()
                .url(transactionConfig.getTransactionUrl() + "/rtc/payment"
                        + "?fromUserAddress=" + fromUserAddress
                        + "&to=" + to
                        + "&password=" + password
                        + "&bTime=" + bTime
                        + "&channel=" + channel)
                .get()
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();

        log.info("Wallet rtcPayment:{}", respStr);
        WalletUserResult result = JSONUtil.toBean(respStr, WalletUserResult.class);
        return result;
    }

    public WalletUserResult noReply(String from, String to) throws Exception {
        Request request = new Request.Builder()
                .url(transactionConfig.getTransactionUrl() + "/chat/noReply"
                        + "?from=" + from
                        + "&to=" + to)
                .get()
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();

        log.info("Wallet NoReply:{}", respStr);
        WalletUserResult result = JSONUtil.toBean(respStr, WalletUserResult.class);
        return result;
    }

    public WalletAssetActivityResult activity(String from, String to, String type) throws Exception {
        Request request = new Request.Builder()
                .url(transactionConfig.getTransactionUrl() + "/user/assetActivity"
                        + "?from=" + from
                        + "&to=" + to
                        + "&type=" + type)
                .get()
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();

        log.info("Wallet activity:{}", respStr);
        WalletAssetActivityResult result = JSONUtil.toBean(respStr, WalletAssetActivityResult.class);
        return result;
    }
//
//    public Response transaction(String orderHash, String userCode, String tokenId, String contractAddress) throws IOException {
//        Request request = new Request.Builder()
//                .url(transactionProperties.getTransactionUrl() + "/order/buy/" + orderHash + "?userId=" + userCode +
//                        "&tokenId=" + tokenId + "&contractAddress=" + contractAddress)
//                .get()
//                .build();
//        return httpClient.newCall(request).execute();
//    }
//
//    public String getSign(String password, String ownerAddress, String contract, String nftId, String nonce) {
//        try {
//            password = URLEncoder.encode(password, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        Request request = new Request.Builder()
//                .url(transactionProperties.getTransactionUrl() + "/user/sign?password=" + password + "&ownerAddress=" + ownerAddress +
//                        "&contract=" + contract + "&nftId=" + nftId + "&nonce=" + nonce
//                )
//                .get()
//                .build();
//        log.info("getSign_url_{}", request.url());
//        Response execute = null;
//        String sign = null;
//        try {
//            execute = httpClient.newCall(request).execute();
//            ResponseBody body = execute.body();
//            sign = body.string();
//            return sign;
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return sign;
//    }
//
//    public Response exchange(String publicAddress, String sourceType, String toType, String amountOfSource) throws IOException {
//        Request request = new Request.Builder()
//                .url(transactionProperties.getTransactionUrl() + "/user/exchange?publicAddress=" + publicAddress + "&sourceType=" + sourceType +
//                        "&toType=" + toType + "&amountOfSource=" + amountOfSource)
//                .get()
//                .build();
//        return httpClient.newCall(request).execute();
//    }
//
//    public Response withdraw(String password,
//                             String userAddress,
//                             String transferAddress,
//                             String type,
//                             String amount, String transactionOrderId) throws IOException {
//        OkHttpClient httpClient1 = new OkHttpClient.Builder()
//                .connectTimeout(50000, TimeUnit.MILLISECONDS)
//                .callTimeout(50000, TimeUnit.MILLISECONDS)
//                .readTimeout(100000, TimeUnit.MILLISECONDS)
//                .retryOnConnectionFailure(false)
//                .build();
//        Request request = new Request.Builder()
//                .url(transactionProperties.getTransactionUrl() + "/user/withdraw/asset?password=" + password + "&userAddress=" + userAddress +
//                        "&transferAddress=" + transferAddress + "&type=" + type + "&amount=" + amount + "&transactionOrderId=" + transactionOrderId)
//                .get()
//                .build();
//        return httpClient1.newCall(request).execute();
//    }
//
//
//    public Response activity(String publicAddress) throws IOException {
//        OkHttpClient httpClient1 = new OkHttpClient.Builder()
//                .connectTimeout(5000, TimeUnit.MILLISECONDS)
//                .callTimeout(5000, TimeUnit.MILLISECONDS)
//                .readTimeout(10000, TimeUnit.MILLISECONDS)
//                .build();
//
//        Request request = new Request.Builder()
//                .url(transactionProperties.getTransactionUrl() + "/user/activity/" + publicAddress)
//                .get()
//                .build();
//        log.info(request.url().url().toString());
//        return httpClient1.newCall(request).execute();
//    }
//
//    public Response lock(String password, String userAddress) throws IOException {
//        Request request = new Request.Builder()
//                .url(transactionProperties.getTransactionUrl() + "/user/lock?password=" + password + "&userAddress=" + userAddress)
//                .get()
//                .build();
//        return httpClient.newCall(request).execute();
//    }
//
//    public Response unlock(String password, String userAddress) throws IOException {
//        Request request = new Request.Builder()
//                .url(transactionProperties.getTransactionUrl() + "/user/unlock?password=" + password + "&userAddress=" + userAddress)
//                .get()
//                .build();
//        return httpClient.newCall(request).execute();
//    }
//
//    //OrderResult
//    public Response offerRegister(String offerId, String contractAddress, String tokenId, String offerType, String publicAddress) throws IOException {
//        Request request = new Request.Builder()
//                .url(transactionProperties.getTransactionUrl() + "/offer/register/"
//                        + offerId
//                        + "?offerId=" + offerId
//                        + "&contractAddress=" + contractAddress
//                        + "&tokenId=" + tokenId
//                        + "&offerType=" + offerType
//                        + "&publicAddress=" + publicAddress)
//                .get()
//                .build();
//        return httpClient.newCall(request).execute();
//    }
//
//    //UserResult
//    public Response offerTransfer(String password, String offerId, String offerType, String fromUserAddress, String toUserAddress, String type, String amount) throws IOException {
//        Request request = new Request.Builder()
//                .url(transactionProperties.getTransactionUrl() + "/user/offer/transfer"
//                        + "?password=" + password
//                        + "&offerId=" + offerId
//                        + "&offerType=" + offerType
//                        + "&fromUserAddress=" + fromUserAddress
//                        + "&toUserAddress=" + toUserAddress
//                        + "&type=" + type
//                        + "&amount=" + amount)
//                .get()
//                .build();
//        return httpClient.newCall(request).execute();
//    }
//
//    public Response withdrawNFTApprove(String password, String userAddress, String contractAddress, String tokenId,
//                                       String orderId, String type, String amount) throws IOException {
//        Request request = new Request.Builder()
//                .url(transactionProperties.getTransactionUrl() + "/user/withdraw/nft"
//                        + "?password=" + password
//                        + "&userAddress=" + userAddress
//                        + "&contractAddress=" + contractAddress
//                        + "&tokenId=" + tokenId
//                        + "&recordingId=" + orderId
//                        + "&type=" + type
//                        + "&amount=" + amount)
//                .get()
//                .build();
//        log.info("withdraw_approve_request_{}",request.url());
//        return httpClient.newCall(request).execute();
//    } //UserResult
//
//    public Response freezeLaunchpad(String password, String userAddress, String amount, String type, String launchpadId) throws IOException {
//        Request request = new Request.Builder()
//                .url(transactionProperties.getTransactionUrl() + "/user/freeze/launchpad"
//                        + "?password=" + password
//                        + "&userAddress=" + userAddress
//                        + "&amount=" + amount
//                        + "&type=" + type
//                        + "&launchpadId=" + launchpadId)
//                .get()
//                .build();
//        return httpClient.newCall(request).execute();
//    }
//
//    public Response unFreezeLaunchpad(String password, String userAddress, String amount, String type, String launchpadId) throws IOException {
//        Request request = new Request.Builder()
//                .url(transactionProperties.getTransactionUrl() + "/user/unfreeze/launchpad"
//                        + "?password=" + password
//                        + "&userAddress=" + userAddress
//                        + "&costedAmount=" + amount
//                        + "&type=" + type
//                        + "&launchpadId=" + launchpadId)
//                .get()
//                .build();
//        log.info("unFreezeLaunchpad_url:{}",request.url());
//        return httpClient.newCall(request).execute();
//    }

}
