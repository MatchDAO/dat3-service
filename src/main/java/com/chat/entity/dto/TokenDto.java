package com.chat.entity.dto;


import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

public class TokenDto implements Serializable {

    public static final long serialVersionUID = 1L;
    /**
     * token
     */
    private String token;


    private String userAccount;
    /**
     * 用户id
     */
    public String userId;
    /**
     * 用户名
     */
    public String userName;
    /**
     * 过期时间
     */
    public long expireTime;
    /**
     * 登录ip
     */
    public String ipaddr;

    public String address;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public Date last;

    public String region;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }


    public Date getLast() {
        return last;
    }

    public void setLast(Date last) {
        this.last = last;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    /**
     * 用户标识
     */
    public String userType;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public String getIpaddr() {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    @Override
    public String toString() {
        return "TokenDto{" +
                "token='" + token + '\'' +
                ", userAccount='" + userAccount + '\'' +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", expireTime=" + expireTime +
                ", ipaddr='" + ipaddr + '\'' +
                ", address='" + address + '\'' +
                ", last=" + last +
                ", region='" + region + '\'' +
                ", userType='" + userType + '\'' +
                '}';
    }
}
