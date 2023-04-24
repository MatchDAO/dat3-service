package com.chat.entity.dto;


import com.chat.utils.StringUtil;

public class CaptchaDto {
    /*
     *账号号码   邮箱/手机号
     */
    private String account;
    /*
     *国家 区号
     */
    private String phoneCode;
    /*
     *账号类型   1:邮箱 2:手机号
     */
    private Integer accountType;

    /*
     *过期时间
     */
    public long expireTime = 60L;


    /*
     *验证码
     */
    private String captcha;

    /*
    验证码类型：register：注册，login：登陆，modify：修改密码， google: 设置google验证码
     */
    private String type;
    private String subject;
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public CaptchaDto(String account) {
        this.account = account;
    }

    public String getPhoneCode() {
        return phoneCode;
    }

    public void setPhoneCode(String phoneCode) {
        this.phoneCode = phoneCode;
    }

    public CaptchaDto() {
    }


    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public String getCaptcha() {
        return captcha;
    }

    public Boolean check(String captcha) {
        if (StringUtil.isEmpty(captcha) || StringUtil.isEmpty(this.captcha)) {
            return false;
        }
        return this.captcha.equals(captcha);
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

    @Override
    public String toString() {
        return "CaptchaDto{" +
                "account='" + account + '\'' +
                ", phoneCode='" + phoneCode + '\'' +
                ", accountType=" + accountType +
                ", expireTime=" + expireTime +
                ", captcha='" + captcha + '\'' +
                ", type='" + type + '\'' +
                '}';
    }


}
