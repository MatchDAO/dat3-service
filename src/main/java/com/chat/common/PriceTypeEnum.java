package com.chat.common;

import lombok.Getter;

@Getter
public enum PriceTypeEnum {
    ETH("ETH", "0x0000000000000000000000000000000000000000"),
    USDCERC("USDCERC", "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"),
    USDTERC("USDTERC", "0x0000000000000000000000000000000000000000"),
    USDTTRC("USDTTRC", "Trx000000000000000000000000000000000000000");

    PriceTypeEnum(String type, String address) {
        this.type = type;
        this.address = address;
    }

    private String type;
    private String address;

    public static PriceTypeEnum of(String type){
        for (PriceTypeEnum value : PriceTypeEnum.values()) {
            if (value.getType().equalsIgnoreCase(type)) {
                return value;
            }
        }
        //参数不合法默认ETH
        return PriceTypeEnum.ETH;
    }
}
