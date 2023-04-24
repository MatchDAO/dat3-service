package com.chat.entity.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

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
public class CreatorSearchDto implements Serializable {

    private static final long serialVersionUID = 1L;


    private String keyword;
    private String profession;
    private String userName;
    private String userCode;
    private String bio;
    private Integer current;
    private Integer size;
    private Integer gender;


}
