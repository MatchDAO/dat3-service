package com.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * <p>
 * 创作者表
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
@Getter
@Setter
@TableName("creator")
public class Creator implements Serializable {

    private static final long serialVersionUID = 1L;


    @TableId("user_code")
    private String userCode;

    @TableField("profession")
    private String profession;


    @TableField("profession_bio")
    private String professionBio;

    @TableField("online")
    private Integer online ;

    @TableField("profit7d")
    private BigInteger profit7d ;

    @TableField("interactive7d")
    private Integer interactive7d ;

    @TableField("interactive")
    private Integer interactive ;

    @TableField("last_online_time")
    private LocalDateTime lastOnlineTime ;


}
