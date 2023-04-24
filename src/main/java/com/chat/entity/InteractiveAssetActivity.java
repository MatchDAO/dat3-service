package com.chat.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 互动资产记录
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
@Data
@TableName("interactive_asset_activity")
@Builder
@ToString

@AllArgsConstructor
@NoArgsConstructor
public class InteractiveAssetActivity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    @TableField("from_address")
    private String fromAddress;

    @TableField("to_address")
    private String toAddress;

    @TableField("token")
    private String token;

    @TableField("amount")
    private String amount;

    @TableField("sent_count")
    private Integer sentCount;

    @TableField("from_profit_token")
    private String fromProfitToken;

    @TableField("from_profit_amount")
    private String fromProfitAmount;

    @TableField("from_spend_amount")
    private String fromSpendAmount;

    @TableField("from_spend_token")
    private String fromSpendToken;

    @TableField("to_spend_token")
    private String toSpendToken;

    @TableField("to_spend_amount")
    private String toSpendAmount;

    @TableField("to_profit_token")
    private String toProfitToken;

    @TableField("to_profit_amount")
    private String toProfitAmount;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;


}
