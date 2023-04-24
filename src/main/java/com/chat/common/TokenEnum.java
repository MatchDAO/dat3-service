package com.chat.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.chat.config.own.PrivateConfig.DAT3;

@Getter
@AllArgsConstructor
public enum TokenEnum {

    ETH("ETH", "ETH", "ETH", "18", "0"),
    APT("APT", "APT", "APTOS", "8", "0x1::aptos_coin::AptosCoin"),
    USDTERC("USDTERC", "USDT", "ETH", "6", "0xdAC17F958D2ee523a2206206994597C13D831ec7"),
    BUSDERC("BUSDERC", "BUSD", "ETH", "18", "0x4Fabb145d64652a948d72533023f6E7A623C7C53"),
    USDCERC("USDCERC", "USDC", "ETH", "6", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"),
    BNBERC("BNBERC", "BNB", "ETH", "18", "0xB8c77482e45F1F44dE1745F52C74426C631bDD52"),

    DAT3APTOS("DAT3", "DAT3", "APTOS", "6", DAT3 +"::dat3_coin::DAT3"),
    ;

    private String token;
    private String symbol;
    private String chain;
    private String decimal;
    private String contract;

    public static String chain(String token) {

        for (TokenEnum tokenEnum : values()) {
            if (tokenEnum.getToken().equalsIgnoreCase(token)) {
                return tokenEnum.getChain();
            }
        }
        return null;

    }

    public static HashMap<String, TokenEnum> getTokens() {

        TokenEnum[] values = TokenEnum.values();
        HashMap<String, TokenEnum> tokenEnumHashMap = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            tokenEnumHashMap.put(values[i].getToken(), values[i]);
        }
        return tokenEnumHashMap;

    }

    public static TokenEnum of(String token) {

        for (TokenEnum tokenEnum : values()) {
            if (tokenEnum.getSymbol().equalsIgnoreCase(token)) {
                return tokenEnum;
            }
        }
        return null;

    }

    public static TokenEnum ofSymbol(String symbol) {

        for (TokenEnum tokenEnum : values()) {
            if (tokenEnum.getSymbol().equalsIgnoreCase(symbol)) {
                return tokenEnum;
            }
        }
        return null;

    }

    public static List<TokenEnum> ofChain(String chain) {
        List<TokenEnum> temp = new ArrayList<>();
        for (TokenEnum tokenEnum : values()) {
            if (tokenEnum.getChain().equalsIgnoreCase(chain)) {
                temp.add(tokenEnum);
            }
        }
        return temp;
    }

}
