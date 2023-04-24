package com.chat.entity.dto;

import lombok.Data;

@Data
public class UserWalletAuth {
    private static final long serialVersionUID = 124985493458739457L;

    //账户
    private String account;
    // 昵称
    private String userName;
    //地区
    private String regions;
    //地区
    private String ipV4;
    private String trx;
    private String invitationCode;
}
