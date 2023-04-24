package com.chat.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.*;

/**
 * <p>
 * 互动表
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("interactive")
public class Interactive implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId()
    private String id;

    @TableField("user_code")
    private String userCode;

    @TableField("profession_type")
    private String professionType;

    @TableField("creator")
    private String creator;

    @TableField("amount")
    private String amount;

    @TableField("token")
    private String token;

    @TableField("status")
    private String status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


    @TableField("reserved")
    private String reserved;

    @TableField("time_millis")
    private Long timeMillis;
}
