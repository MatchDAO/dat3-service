package com.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author jetBrains
 * @since 2022-12-21
 */
@Getter
@Setter
@TableName("invitation_rewards")
public class InvitationRewards implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    @TableField("user_code")
    private String userCode;

    /**
     * 邀请码
     */
    @TableField("invitation_code")
    private String invitationCode;

    /**
     * 邀请码索引
     */
    @TableField("`index`")
    private Integer index;

    /**
     * 被邀请人
     */
    @TableField("`invited`")
    private String invited;

    /**
     * 消耗或赚取单位wei
     */
    @TableField("`type`")
    private String type;

    /**
     * 奖励 单位wei
     */
    @TableField("`rewards`")
    private String rewards;
    @TableField("`amount`")
    private String amount;

    /**
     * 代币
     */
    @TableField("`token`")
    private String token;
    @TableField("`create_time`")
    private LocalDateTime createTime;


}
