package com.chat.common;

import lombok.Getter;

@Getter
public enum ActivityType {
    SEND_MSG("SEND_MSG"),
    REPLY_MSG("REPLY_MSG"),
    NO_REPLY("NO_REPLY"),
    WITHDRAW("WITHDRAW"),
    TRANSFER_IN("TRANSFER_IN"),
    TRANSFER("TRANSFER"),
    UNFREEZE_ASSET("UNFREEZE_ASSET"),
    FREEZE_ASSET("FREEZE_ASSET"),
    EXCHANGE("EXCHANGE"),
    DEPOSIT("DEPOSIT"),
    REWARD_DAT3("REWARD_DAT3"),
    ;

    private String name;

    ActivityType(String name) {
        this.name = name;
    }
}
