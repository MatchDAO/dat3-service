
package com.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 文件管理
 *
 * @author Luckly
 * @date 2019-06-18 17:18:42
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("sys_file")
public class SysFile extends Model<SysFile> {

	private static final long serialVersionUID = 1L;

	/**
	 * 编号
	 */
	@TableId(type = IdType.AUTO)
	private Long id;

	/**
	 * 文件名
	 */
	private String fileName;

	/**
	 * 原文件名
	 */
	private String original;

	/**
	 * 容器名称
	 */
	private String bucketName;

	/**
	 * 文件类型
	 */
	private String type;

	/**
	 * 文件大小
	 */
	private Long fileSize;

	/**
	 * 上传人
	 */
	private String createUser;

	/**
	 * 上传时间
	 */
	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createTime;


	/**
	 * 更新时间
	 */
	@TableField(fill = FieldFill.UPDATE)
	private LocalDateTime updateTime;

	/**
	 * 删除标识：1-删除，0-正常
	 */
	@TableField(fill = FieldFill.INSERT)
	private String delFlag;
	private Integer sort;

}
