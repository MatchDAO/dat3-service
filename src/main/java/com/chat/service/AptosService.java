package com.chat.service;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.chat.entity.dto.AptosRequestTask;
import com.chat.utils.aptos.AptosClient;
import com.chat.utils.aptos.request.v1.model.Response;
import com.chat.utils.aptos.request.v1.model.Transaction;
import com.chat.utils.aptos.request.v1.model.TransactionPayload;
import com.chat.utils.aptos.request.v1.model.TransactionViewPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.chat.config.own.PrivateConfig.DAT3;
import static com.chat.config.own.PrivateConfig.DAT3_NFT;

@Slf4j
@Component
public class AptosService {

    public BlockingQueue<AptosRequestTask> task = new LinkedBlockingQueue<AptosRequestTask>();
    @Resource
    private AptosClient aptosClient;


    public void toQueue(String sin, TransactionPayload payload, boolean simulate) {
        task.add(new AptosRequestTask(sin, payload, simulate));
    }

    public boolean checkUser(String address) {
        ;
        return getUserAssets(address) != null;
    }

    public JSONArray getUserAssets(String address) {
        try {
            TransactionViewPayload viewPayload = TransactionViewPayload.builder()
                    .function(DAT3 + "::routel::assets")
                    .arguments(Collections.singletonList(address))
                    .typeArguments(Collections.emptyList())
                    .build();
            Response<JSON> view = aptosClient.view(null, viewPayload);
            if (JSONArray.class.equals(view.getData().getClass())) {
                return JSONUtil.parseArray(view.getData());
            }
        } catch (Exception e) {
            log.error("getUserAssets");
            return null;
        }
        log.error("getUserAssets");
        return null;
    }

    public Response<Transaction> dat3_manager_sys_user_init(String fid, String uid, String address) {
        try {
            ArrayList arguments = new ArrayList<>();
            arguments.add(fid);
            arguments.add(uid);
            arguments.add(address);
            TransactionPayload transactionPayload = TransactionPayload.builder()
                    .type(TransactionPayload.ENTRY_FUNCTION_PAYLOAD)
                    .function(DAT3 + "::routel::sys_user_init")
                    .arguments(arguments)
                    .typeArguments(Collections.emptyList())
                    .build();

            return aptosClient.requestSubmitTransaction(DAT3, transactionPayload);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("getUserAssets");
            return null;
        }
    }

    public void dat3_routel_send_msg(String from, String to, Integer count, String sender) {
        try {
            ArrayList arguments = new ArrayList<>();
            arguments.add(from);
            arguments.add(to);
            arguments.add("" + count);
            arguments.add("0");
            TransactionPayload transactionPayload = TransactionPayload.builder()
                    .type(TransactionPayload.ENTRY_FUNCTION_PAYLOAD)
                    .function(DAT3 + "::routel::send_msg")
                    .arguments(arguments)
                    .typeArguments(Collections.emptyList())
                    .build();

            toQueue(DAT3, transactionPayload, "0".equals(sender));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("getUserAssets");
        }
    }

    public JSONArray getUserCall(String address) {
        TransactionViewPayload viewPayload = TransactionViewPayload.builder()
                .function(DAT3 + "::routel::remaining_time")
                .arguments(Collections.singletonList(address))
                .typeArguments(Collections.emptyList())
                .build();
        Response<JSON> view = aptosClient.view(null, viewPayload);
        log.info("getUserAssets:" + view);
        if (JSONArray.class.equals(view.getData().getClass())) {

            return JSONUtil.parseArray(view.getData()).getJSONArray(0);
        }
        log.error("getUserAssets");
        return null;
    }

    public Response<Transaction> dat3_manager_mint_to() {
        try {
            TransactionPayload transactionPayload = TransactionPayload.builder()
                    .type(TransactionPayload.ENTRY_FUNCTION_PAYLOAD)
                    .function(DAT3 + "::dat3_core::mint_to")
                    .arguments(Collections.emptyList())
                    .typeArguments(Collections.emptyList())
                    .build();

            return aptosClient.requestSubmitTransaction(DAT3, transactionPayload);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("getUserAssets");
            return null;
        }
    }


    public JSONArray view_call_1(String address, String to) {

        ArrayList arguments = new ArrayList<>();
        arguments.add(address);
        arguments.add(to);
        TransactionViewPayload viewPayload = TransactionViewPayload.builder()
                .function(DAT3 + "::routel::view_call_1")
                .arguments(arguments)
                .typeArguments(Collections.emptyList())
                .build();
        Response<JSON> view = aptosClient.view(null, viewPayload);
        log.error("view_call_1" + view);
        if (JSONArray.class.equals(view.getData().getClass())) {
            return JSONUtil.parseArray(view.getData()).getJSONArray(0);
        }
        return null;
    }

    public Response<Transaction> sys_call_1(String address, String to, String gas) throws InterruptedException {

        ArrayList arguments = new ArrayList<>();
        arguments.add(address);
        arguments.add(to);
        arguments.add(gas);
        TransactionPayload viewPayload = TransactionPayload.builder()
                .type(TransactionPayload.ENTRY_FUNCTION_PAYLOAD)
                .function(DAT3 + "::routel::sys_call_1")
                .arguments(arguments)
                .typeArguments(Collections.emptyList())
                .build();
        try {
            return aptosClient.requestSubmitTransaction(DAT3, viewPayload);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public JSONArray getMint() {
        TransactionViewPayload viewPayload = TransactionViewPayload.builder()
                .function(DAT3 + "::dat3_core::genesis_info")
                .arguments(Collections.emptyList())
                .typeArguments(Collections.emptyList())
                .build();
        Response<JSON> view = aptosClient.view(null, viewPayload);
        log.error("getMint" + view);
        if (JSONArray.class.equals(view.getData().getClass())) {
            return JSONUtil.parseArray(view.getData());
        }
        return null;
    }

    public Object getBalance(String account) {
        TransactionViewPayload viewPayload = TransactionViewPayload.builder()
                .function(DAT3 + "::dat3_core::genesis_info")
                .arguments(Collections.emptyList())
                .typeArguments(Collections.emptyList())
                .build();
        return aptosClient.requestAccountResource(account, com.chat.utils.aptos.request.v1.model.Resource.apt());


    }

    public JSONArray fid_reward(String fid) {
        TransactionViewPayload viewPayload = TransactionViewPayload.builder()
                .function(DAT3 + "::routel::fid_reward")
                .arguments(Collections.singletonList(fid))
                .typeArguments(Collections.emptyList())
                .build();
        Response<JSON> view = aptosClient.view(null, viewPayload);
        log.error("getMint" + view);
        if (JSONArray.class.equals(view.getData().getClass())) {
            return JSONUtil.parseArray(view.getData());
        }
        return null;
    }

    public JSONArray get_nft_mint(String address, String collection_name) {
        ArrayList arguments = new ArrayList<>();
        arguments.add(address);
        arguments.add(collection_name);
        TransactionViewPayload viewPayload = TransactionViewPayload.builder()
                .function(DAT3_NFT + "::dat3_invitation_nft::mint_state")
                .arguments(arguments)
                .typeArguments(Collections.emptyList())
                .build();
        Response<JSON> view = aptosClient.view(null, viewPayload);
        log.error("get_nft_mint" + view);
        if (JSONArray.class.equals(view.getData().getClass())) {
            return JSONUtil.parseArray(view.getData());
        }
        return null;
    }

    public JSONObject getEpochInfo() {
        com.chat.utils.aptos.request.v1.model.Resource blockResourceTag = com.chat.utils.aptos.request.v1.model.Resource.builder()
                .moduleAddress("0x1")
                .moduleName("block")
                .resourceName("BlockResource")
                .build();
        com.chat.utils.aptos.request.v1.model.Resource confResourceTag = com.chat.utils.aptos.request.v1.model.Resource.builder()
                .moduleAddress("0x1")
                .moduleName("reconfiguration")
                .resourceName("Configuration")
                .build();
        JSONObject block = aptosClient.requestAccountResource("0x1", blockResourceTag).getData().getData();
        JSONObject conf = aptosClient.requestAccountResource("0x1", confResourceTag).getData().getData();

        Long base_epoch_interval = 7200000000L;
        if (aptosClient.host().contains("testnet")) {
            base_epoch_interval = 3600000000L;
        }
        Long epochInterval = block.getLong("epoch_interval", 3600000000L) / 1000000;
        Long epoch = conf.getLong("epoch");
        Long currentEpochStartTime = conf.getLong("last_reconfiguration_time") / 1000000;
        Long nextEpochStartTime = currentEpochStartTime + epochInterval;
        JSONObject res = new JSONObject();
        res.set("epochInterval", epochInterval);
        res.set("epoch", epoch);
        res.set("currentEpochStartTime", currentEpochStartTime);
        res.set("nextEpochStartTime", nextEpochStartTime);
        return res;
    }

}
