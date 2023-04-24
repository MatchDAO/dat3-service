package com.chat.common;

import lombok.Getter;

@Getter
public enum ConsumerEnum {
    SPEND("SPEND"   ),
    EARN("EARN"  ),
    ;

    private final String sign;

    ConsumerEnum(String sign  ) {
        this.sign = sign;
    }

}
