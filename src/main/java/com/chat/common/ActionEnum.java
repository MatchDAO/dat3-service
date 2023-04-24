package com.chat.common;

import lombok.Getter;

@Getter
public enum ActionEnum {
    SIGN_IN("SIGN_IN", 2 ),
    SEND_MSG("SEND_MSG", 1 ),
    CUSTOM("CUSTOM", 0 ),
    UNKNOWN("UNKNOWN", 1 ),
    ;

    private  String sign;
    private  Integer add;

    ActionEnum(String sign,Integer add) {
        this.sign = sign;
        this.add = add;
    }

    public static ActionEnum of(String action) {

        for (ActionEnum actionEnum : values()) {
            if (actionEnum.getSign().equalsIgnoreCase(action)) {
                return actionEnum;
            }
        }
        return ActionEnum.UNKNOWN;

    }
}
