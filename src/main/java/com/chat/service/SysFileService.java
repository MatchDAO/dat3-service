/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.common.R;
import com.chat.entity.SysFile;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 文件管理
 *
 * @author Luckly
 * @date 2019-06-18 17:18:42
 */
public interface SysFileService extends IService<SysFile> {

	/**
	 * 上传文件
	 * @param file
	 * @return
	 */
	Map<String,Object> uploadFile(MultipartFile file, String userId ,Integer sort);
	LinkedList<Map<String, Object>> uploadFiles(MultipartFile[] files, String userId,int sort);
	LinkedList<Map<String, Object>> getFilesUrl(String userId);
	Map<String, Object> uploadFile(InputStream inputStream,String OriginalFilename,String fileName,String contentType, String userId);

	/**
	 * 读取文件
	 * @param bucket 桶名称
	 * @param fileName 文件名称
	 * @param response 输出流
	 */
	void getFile(String bucket, String fileName, HttpServletResponse response);

	/**
	 * 删除文件
	 * @param id
	 * @return
	 */
	Boolean deleteFile(Long id,String fileName);

    String getFileUrl(String fileName);

	Boolean modifyFiles(List<SysFile> files);
}
