package com.chat.entity.dto;

import lombok.Data;


@Data
public class UserAuthDto {
    private static final long serialVersionUID = 124985493458739457L;

    //账户
    private String account;
    // 昵称
    private String userName;
    // 密码
    private String password;
    //地区
    private String regions;
    //地区
    private String ipV4;
    // 账户类型:1邮箱,2手机号)
    private Integer accountType;
    // 验证码
    private String captcha;
    private String invitation;


}
