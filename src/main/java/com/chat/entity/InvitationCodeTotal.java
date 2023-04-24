package com.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 
 * </p>
 *
 * @author jetBrains
 * @since 2022-12-21
 */
@Data
@TableName("invitation_code_total")
public class InvitationCodeTotal implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("user_code")
    private String userCode;

    @TableField("`total`")
    private Integer total;

    @TableField("`used`")
    private Integer used;


}
