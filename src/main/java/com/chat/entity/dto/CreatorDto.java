package com.chat.entity.dto;

import java.io.Serializable;

/**
 * <p>
 * 创作者表
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */

public class CreatorDto implements Serializable {

    private static final long serialVersionUID = 135346457556545L;


    private String userCode;

    public Boolean getShow() {
        return show;
    }

    public void setShow(Boolean show) {
        this.show = show;
    }

    private Boolean show;


    private String address;
    private String userName;
    private String portrait;



    private String email;


    private String bio;

    private String profession;


    private String professionBio;

    private Long online;
    private Integer gender;



    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPortrait() {
        return portrait;
    }

    public void setPortrait(String portrait) {
        this.portrait = portrait;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public String getProfessionBio() {
        return professionBio;
    }

    public void setProfessionBio(String professionBio) {
        this.professionBio = professionBio;
    }

    public Long getOnline() {
        return online;
    }

    public void setOnline(Long online) {
        this.online = online;
    }
}
