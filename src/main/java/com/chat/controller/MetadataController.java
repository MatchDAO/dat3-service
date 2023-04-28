package com.chat.controller;

import cn.hutool.json.JSONObject;
import com.chat.common.AuthToken;
import com.chat.common.ChainEnum;
import com.chat.common.R;
import com.chat.common.TokenEnum;
import com.chat.service.TransactionUtils;
import com.chat.service.impl.MetadataServiceImpl;
import com.chat.service.impl.PriceRangeServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.chat.config.ChatConfig.*;

/**
 * 核心数据
 */
@RestController
@RequestMapping("/metadata")
@Log4j2
@AuthToken
public class MetadataController {
    @Resource
    private MetadataServiceImpl metadataService;


    @GetMapping("/chatFee")
    public R chatFee() {
        return metadataService.chatFee();
    }

    @AuthToken(validate = false)
    @GetMapping("/base")
    public R base() {
        JSONObject entries = new JSONObject();
        entries.set("appVerify", "1");
        entries.set("v", "1.1.0");
        entries.set("twitter", TWITTER);
        entries.set("discord", DISCORD);
        entries.set("email", EMAIL);
        entries.set("github", GITHUB);
        entries.set("website", HOME);
        return R.success(entries);
    }

    @GetMapping("/chains")
    public R chain() {
        ChainEnum[] values = ChainEnum.values();
        List<HashMap<String, Object>> chains = new ArrayList<>();
        for (ChainEnum value : values) {
            HashMap<String, Object> temp = new HashMap<>();
            temp.put("name", value.getName());
            temp.put("token", value.getToken());
            temp.put("compatible", value.getCompatible());
            temp.put("code", value.getCode());
            temp.put("chainDefault", value.getChainDefault());
            temp.put("lock", value.getLock());
            temp.put("icon", value.getIcon());
            temp.put("scanUrl", value.getScanUrl());
            temp.put("rpcRrl", value.getRpcUrl());
            List<TokenEnum> tokens = TokenEnum.ofChain(value.getName());
            List<HashMap<String, Object>> tokenList = new ArrayList<>();
            for (TokenEnum token : tokens) {
                HashMap<String, Object> temp1 = new HashMap<>();
                temp1.put("token", token.getToken());
                temp1.put("symbol", token.getSymbol());
                temp1.put("chain", token.getChain());
                temp1.put("decimal", token.getDecimal());
                temp1.put("contract", token.getContract());
                tokenList.add(temp1);
            }
            temp.put("tokens", tokenList);
            chains.add(temp);
        }
        return R.success(chains);
    }

    @GetMapping("/refresh")
    public String get(HttpSession session) {
        return "Processed";
    }

    // @AuthToken(validate = false)
    @GetMapping("/grade")
    public R grade(HttpSession session) throws Exception {
        return metadataService.grade();
    }
}