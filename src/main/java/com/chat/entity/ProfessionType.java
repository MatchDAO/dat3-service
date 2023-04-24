package com.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 创作者专业类型
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
@Getter
@Setter
@TableName("profession_type")
public class ProfessionType implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("tag")
    private String tag;

    @TableField("description")
    private String description;


}
