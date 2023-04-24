package com.chat.entity.dto;

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
public class InvitationRewardsDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */

    private String userCode;

    /**
     * 邀请码
     */

    private String invitationCode;

    /**
     * 邀请码索引
     */

    private Integer index;

    /**
     * 被邀请人
     */

    private String invited;
    private String userName;
    private String portrait;
    /**
     * 消耗或赚取单位wei
     */

    private String type;

    /**
     * 奖励 单位wei
     */

    private String rewards;

    private String amount;

    /**
     * 代币
     */

    private String token;

    private LocalDateTime createTime;


}
