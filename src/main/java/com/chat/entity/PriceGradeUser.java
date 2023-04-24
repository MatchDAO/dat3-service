package com.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("price_grade_user")
public class PriceGradeUser {
    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("range_id")
    private Integer rangeId;
    @TableField("user_code")
    private String userCode;
}
