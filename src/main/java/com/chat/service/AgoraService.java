package com.chat.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.chat.config.own.PrivateConfig;
import com.chat.utils.agora.media.RtcTokenBuilder2;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;
import org.web3j.abi.datatypes.Address;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
@Component
public class AgoraService {
    static String appId = PrivateConfig.AGORA_APP_ID;
    static String appCertificate = PrivateConfig.AGORA_APP_CERT;
    // 客户 ID
    final String APIKey = PrivateConfig.AGORA_API_KEY;
    // 客户密钥
    final String APISecret = PrivateConfig.AGORA_API_SECRET;
    String authorizationHeader = "Basic " + new String(Base64.getEncoder().encode((APIKey + ":" + APISecret).getBytes()));
    private final static OkHttpClient httpClient = new OkHttpClient();

    //单用户是否在通道中
    public boolean userInChannel(String uid, String channelName) throws IOException {
        Request request = new Request.Builder()
                .url("https://api.agora.io/dev/v1/channel/user/property/" + appId + "/" + uid + "/" + channelName)
                .get()
                .header("Authorization", authorizationHeader)
                .header("Content-Type", "application/json")
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();
        log.info("AgoraService userInChannel:{}-----------resp:{}", respStr, resp);
        if (resp.code() == 200) {
            JSONObject entries = JSONUtil.parseObj(respStr);
            return entries.getJSONObject("data").getBool("in_channel");
        }
        return false;
    }

    public boolean suspendChannel(String channelName,Integer seconds) throws IOException {
        JSONObject param = new JSONObject();
        param.set("cname",channelName);
        param.set("time_in_seconds",seconds);
        param.set("appid",appId);
        param.set("privileges", Arrays.asList("join_channel") );
        Request request = new Request.Builder()
                .url("https://api.agora.io/dev/v1/kicking-rule")
                .post(okhttp3.RequestBody.create(param.toString(), MediaType.parse("application/json; charset=utf-8")))
                .header("Authorization", authorizationHeader)
                .header("Content-Type", "application/json")
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();
        log.info("AgoraService destroyChannel:{}-----------resp:{}", respStr, resp);
        if (resp.code() == 200) {
            JSONObject entries = JSONUtil.parseObj(respStr);
            return "success".equalsIgnoreCase(entries.getStr("status",""));
        }


        return true;
    }
    public JSONObject ruleChannel() throws IOException {
        Request request = new Request.Builder()
                .url("https://api.agora.io/dev/v1/kicking-rule?appid="+appId)
                .get()
                .header("Authorization", authorizationHeader)
                .header("Content-Type", "application/json")
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();
        log.info("AgoraService ruleChannel:{}-----------resp:{}", respStr, resp);
        if (resp.code() == 200) {
            JSONObject entries = JSONUtil.parseObj(respStr);
            return entries;
        }


        return null;
    }


    //当前通道所有用户
    public JSONArray usersInChannel(String channelName) throws IOException {
        //https://api.agora.io/dev/v1/channel/user/{appid}/{channelName}
        Request request = new Request.Builder()
                .url("https://api.agora.io/dev/v1/channel/user/" + appId + "/" + channelName)
                .get()
                .header("Authorization", authorizationHeader)
                .header("Content-Type", "application/json")
                .build();
        Response resp = httpClient.newCall(request).execute();
        String respStr = resp.body().string();
        log.info("AgoraService usersInChannel:{}-----------resp:{}", respStr, resp);
        if (resp.code() == 200) {
            JSONObject entries = JSONUtil.parseObj(respStr);
            if (entries.getBool("success", false) && entries.getJSONObject("data").getBool("channel_exist")) {
                return entries.getJSONObject("data").getJSONArray("users");
            }
        }
        return null;
    }


    // 创建 authorization header

    public String rtcTokenBuilder(int uid, String channelName, int privilegeExpirationInSeconds) {
        log.info("{},{},{}",uid,channelName,privilegeExpirationInSeconds);
        RtcTokenBuilder2 token = new RtcTokenBuilder2();
        String result = token.buildTokenWithUid(appId, appCertificate, channelName, uid, privilegeExpirationInSeconds, privilegeExpirationInSeconds, privilegeExpirationInSeconds, privilegeExpirationInSeconds, privilegeExpirationInSeconds);
        return result;
    }
}
