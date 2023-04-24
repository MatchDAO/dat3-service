package com.chat.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.*;

/**
 * <p>
 * 资产变动表
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("asset_activity")
public class AssetActivity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId()
    private String id;

    @TableField("from_address")
    private String fromAddress;

    @TableField("to_address")
    private String toAddress;

    @TableField("amount")
    private String amount;

    @TableField("token")
    private String token;
    @TableField("chain")
    private String chain;

    @TableField("type")
    private String type;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("transaction_hash")
    private String transactionHash;
    @TableField("confirm_state")
    private String confirmState;



}
