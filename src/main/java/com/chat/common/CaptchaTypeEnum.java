package com.chat.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 1
 */

@Getter
@AllArgsConstructor
public enum CaptchaTypeEnum {
    /**
     * 验证码类型 1 登陆 2修改密码  3 注册登陆 4 注册 5 提现 6nft提现 8 其他
     */
    LOGIN("1", "login"),
    MODIFY("2", "modify"),
    LOGIN_REGISTER("3", "login_register"),
    REGISTER("4", "register"),
    WITHDRAW("5", "withdraw"),
    CODE("8", "code");
    /**
     * 状态值
     */
    private final String type;

    /**
     * 描述
     */
    private final String sign;

    /**
     * 自己定义一个静态方法,通过value返回枚举常量对象
     * @param sign
     * @return
     */
    public static CaptchaTypeEnum ofSign(String sign){

        for (CaptchaTypeEnum action: values()) {
            if(action.getSign().equals(sign)){
                return  action;
            }
        }
        return CaptchaTypeEnum.CODE;

    }
    public static CaptchaTypeEnum of(String type){

        for (CaptchaTypeEnum action: values()) {
            if(action.getType().equals(type)){
                return  action;
            }
        }
        return CaptchaTypeEnum.CODE;

    }
}
