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

package com.chat.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chat.common.AuthToken;
import com.chat.common.R;
import com.chat.common.SecurityConstant;
import com.chat.entity.SysFile;
import com.chat.entity.User;
import com.chat.service.SysFileService;
import com.chat.service.impl.SysFileServiceImpl;
import com.chat.service.impl.UserServiceImpl;
import com.chat.utils.JwtUtils;
import com.chat.utils.MessageUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

import static com.chat.service.impl.SysFileServiceImpl.awsUrl;

/**
 * 文件管理
 */
@RestController
@AllArgsConstructor
@RequestMapping("/resources")
@AuthToken
@Slf4j
public class SysFileController {

    @Resource
    private SysFileService sysFileService;
    @Resource
    private SysFileServiceImpl sysFileService1;
    @Resource
    private UserServiceImpl userService;

    /**
     * 分页查询
     *
     * @param page    分页对象
     * @param sysFile 文件管理
     * @return
     */
    @GetMapping("/page")
    public R getSysFilePage(Page page, SysFile sysFile) {
        return R.success(sysFileService.page(page, Wrappers.query(sysFile)));
    }

//	/**
//	 * 通过id删除文件管理
//	 * @param id id
//	 * @return R
//	 */
//	@DeleteMapping("/{id}")
//	public R removeById(@PathVariable Long id) {
//		return R.success(sysFileService.deleteFile(id));
//	}


    /**
     * 获取文件
     *
     * @param bucket   桶名称
     * @param fileName 文件空间/名称
     * @param response
     * @return
     */
    @AuthToken(validate = false)
    @GetMapping("/{bucket}/{fileName}")
    public void file(@PathVariable String bucket, @PathVariable String fileName, HttpServletResponse response) {
        sysFileService.getFile(bucket, fileName, response);
    }

    @AuthToken(validate = false)
    @PostMapping("/uploadFiles/{userCode}")
    public R uploadFiles(@PathVariable String userCode, @RequestParam(value = "files", required = false) MultipartFile[] files, @RequestHeader(value = "token") String token) {

        String userCode1 = null;
        try {
            userCode1 = JwtUtils.getUser(token, SecurityConstant.USER_ID);
            if (userCode1 == null) {
                return R.success(0);
            }
        } catch (Exception e) {
            log.error("`Invalid user" + e.fillInStackTrace());
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        if (StrUtil.isEmpty(userCode) || StrUtil.isEmpty(userCode1) || !userCode1.equals(userCode)) {
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        User byId = userService.getById(userCode);
        if (byId == null) {
            return R.error("`NO user information");
        }
        if (files == null || files.length < 1) {
            return R.error("`Invalid files");
        }
        List<SysFile> list = sysFileService.list(Wrappers.<SysFile>lambdaQuery()
                .eq(SysFile::getCreateUser, userCode)
                .ne(SysFile::getDelFlag, "1")
                .groupBy(SysFile::getSort));
        int sort = 0;
        if (CollUtil.isNotEmpty(list)) {
            sort = list.get(list.size() - 1).getSort();
            if ((list.size() + files.length) > 6) {
                return R.error("Sorry, you have exceeded the upload limit:" + (list.size() + files.length));
            }
        }
        //上传
        LinkedList<Map<String, Object>> maps = sysFileService1.uploadFiles(files, userCode, sort);
        //上传前为空
        if (CollUtil.isEmpty(list)) {
            //上传后
            List<SysFile> list1 = sysFileService.list(Wrappers.<SysFile>lambdaQuery()
                    .eq(SysFile::getCreateUser, userCode)
                    .ne(SysFile::getDelFlag, "1")
                    .groupBy(SysFile::getSort));
            //上传后不为空
            if (CollUtil.isNotEmpty(list1)) {
                userService.modifyUserInfoBase(byId.getUserCode(), null, String.format("%s%s", awsUrl, list1.get(0).getFileName()));
            }
        }
        return R.success(maps);
    }

    @AuthToken(validate = false)
    @GetMapping("/getFiles/{userCode}")
    public R getFiles(@PathVariable String userCode, @RequestHeader(value = "token") String token) {

        String userCode1 = null;
        try {
            userCode1 = JwtUtils.getUser(token, SecurityConstant.USER_ID);
            if (userCode1 == null) {
                return R.success(0);
            }
        } catch (Exception e) {
            log.error("`Invalid user" + e.fillInStackTrace());
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        if (StrUtil.isEmpty(userCode) || StrUtil.isEmpty(userCode1)) {
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        User byId = userService.getById(userCode);
        if (byId == null) {
            return R.error("`NO user information");
        }
        return R.success(sysFileService1.getFilesUrl(userCode));
    }

    @AuthToken(validate = false)
    @PostMapping("/modify/{userCode}")
    public R modifyUserPortrait(@PathVariable String userCode, @RequestBody List<SysFile> list, @RequestHeader(value = "token") String token) throws Exception {
        String userCode1 = null;
        log.info("/modify/ " + list);
        try {
            userCode1 = JwtUtils.getUser(token, SecurityConstant.USER_ID);
            if (userCode1 == null) {
                return R.success(0);
            }
        } catch (Exception e) {
            log.error("`Invalid user" + e.fillInStackTrace());
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        if (StrUtil.isEmpty(userCode) || StrUtil.isEmpty(userCode1) || !userCode1.equals(userCode)) {
            return R.error(MessageUtils.getLocale("user.invalid"));
        }
        User byId = userService.getById(userCode);
        if (byId == null) {
            return R.error("`NO user information");
        }
        //修改前
        List<SysFile> files = sysFileService.list(Wrappers.<SysFile>lambdaQuery()
                .eq(SysFile::getCreateUser, userCode)
                .ne(SysFile::getDelFlag, "1"));
        List<SysFile> sysFiles = new ArrayList<>();
        Boolean aBoolean = false;
        if (CollUtil.isNotEmpty(list)) {
            for (SysFile sysFile : list) {
                if (sysFile.getId() > 0
                        && (("1".equals(sysFile.getDelFlag())) || sysFile.getSort() != null)
                        && files.stream().anyMatch(s -> Objects.equals(s.getId(), sysFile.getId()))) {
                    SysFile sysFile1 = new SysFile();
                    sysFile1.setId(sysFile.getId());
                    sysFile1.setDelFlag(sysFile.getDelFlag());
                    sysFile1.setSort(sysFile.getSort());
                    sysFiles.add(sysFile1);
                }
            }
            if (CollUtil.isEmpty(sysFiles)) {
                return R.success("`NO file information");
            }
            aBoolean = sysFileService1.modifyFiles(sysFiles);
        }

        List<SysFile> files2 = sysFileService.list(Wrappers.<SysFile>lambdaQuery()
                .eq(SysFile::getCreateUser, userCode)
                .ne(SysFile::getDelFlag, "1"));
        User user = new User();
        user.setUserCode(byId.getUserCode());
        //修改后为空 则无头像
        if (CollUtil.isEmpty(files2)) {
            userService.modifyUserInfoBase(byId.getUserCode(), null, "default");
        }
        List<SysFile> collect1 = files.stream().filter(s -> s.getSort() != null).sorted(Comparator.comparing(SysFile::getSort)).collect(Collectors.toList());
        List<SysFile> collect2 = files2.stream().filter(s -> s.getSort() != null).sorted(Comparator.comparing(SysFile::getSort)).collect(Collectors.toList());
        //修改前为空
        if (CollUtil.isEmpty(files)) {
            //修改后不为空 则头像为第一个
            if (CollUtil.isNotEmpty(files2)) {
                userService.modifyUserInfoBase(byId.getUserCode(), null, String.format("%s%s", awsUrl, collect2.get(0).getFileName()));

            } else {//修改后为空
                userService.modifyUserInfoBase(byId.getUserCode(), null, "default");
            }
        }
        //修改前与修改后都不为空
        if (CollUtil.isNotEmpty(files) && CollUtil.isNotEmpty(files2)) {

            if (CollUtil.isNotEmpty(collect1) && CollUtil.isNotEmpty(collect2)) {
                //排第一的是否相同 不相同修改头像为上传后排第一个
                if (!Objects.equals(collect1.get(0).getId(), collect2.get(0).getId())) {
                    userService.modifyUserInfoBase(byId.getUserCode(), null,String.format("%s%s", awsUrl, collect2.get(0).getFileName()));
                }

            }
        }
        //return ;
        return R.success(aBoolean);
    }


}
