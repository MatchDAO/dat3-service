package com.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author jetBrains
 * @since 2022-10-20
 */
@Data
@TableName("users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("user_code")
    private String userCode;

    @TableField("user_name")
    private String userName;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("password")
    private String password;

    @TableField("mac")
    private String mac;

    @TableField("wallet")
    private String wallet;

    @TableField("portrait")
    private String portrait;

    @TableField("status")
    private String status;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("invitation_code")
    private String invitationCode;

    @TableField("last_access_uuid")
    private String lastAccessUuid;
    @TableField("em_uuid")
    private String emUuid;
    @TableField("bio")
    private String bio;
    @TableField("tag")
    private String tag;
    @TableField("registered")
    private Integer registered;
    @TableField("address")
    private String address;
    /* 0:不公开/保密;1:男;2:女;3:非二元*/
    @TableField("gender")
    private Integer gender;


}
