package com.chat.common;

import lombok.Getter;

@Getter
public enum BotCodeEnum {
    MINT("/mint", " " ),
    CONFIRM("/confirm", "" ),
    UNKNOWN("/", "" ),
    ;

    private  String  code ;
    private  String desc;

    BotCodeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static BotCodeEnum of(String code) {

        for (BotCodeEnum actionEnum : values()) {
            if (actionEnum.getCode().equalsIgnoreCase(code)) {
                return actionEnum;
            }
        }
        return BotCodeEnum.UNKNOWN;

    }
}
