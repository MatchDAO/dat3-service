package com.chat.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InteractiveStautsEnum {
    CALL_BEGIN("CALL_BEGIN", " "),
    CALL_ONCE("CALL_ONCE", " "),
    CALL_ACCEPT("CALL_ACCEPT", " "),
    CALL_END("CALL_END", " "),
    SEND("SEND", " "),
    REPLY("REPLY", " "),
    OVERTIME("OVERTIME", " ");


    private final String type;
    private final String desc;


}
