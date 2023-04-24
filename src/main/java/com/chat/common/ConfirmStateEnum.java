package com.chat.common;

import lombok.Getter;

@Getter
public enum ConfirmStateEnum {
    APPROVING("0", "Approving"),
    PENDING("2", "Pending"),
    SUCCESS("1", "Success"),
    FAILED("3", "failed"),
    REFUSE("4", "refuse"),
    ;

    ConfirmStateEnum(String type, String sign) {
        this.type = type;
        this.sign = sign;
    }

    public static ConfirmStateEnum of(String type) {

        for (ConfirmStateEnum action : values()) {
            if (action.getType().equals(type)) {
                return action;
            }
        }
        return FAILED;

    }

    private final String type;
    private final String sign;
}
