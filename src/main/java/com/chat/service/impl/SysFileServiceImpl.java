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
package com.chat.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.amazonaws.services.s3.model.S3Object;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.config.ChatConfig;
import com.chat.config.OssProperties;
import com.chat.entity.SysFile;
import com.chat.mapper.SysFileMapper;
import com.chat.service.OssTemplate;
import com.chat.service.SysFileService;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件管理
 *
 * @author Luckly
 * @date 2019-06-18 17:18:42
 */
@Slf4j
@Service
@AllArgsConstructor
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements SysFileService {

    private final OssProperties ossProperties;

    private final OssTemplate minioTemplate;

    /**
     * 上传文件
     *
     * @param file
     * @return
     */
    @Override
    public Map<String, Object> uploadFile(MultipartFile file, String userId ,Integer sort) {
        String fileName = IdUtil.simpleUUID() + StrUtil.DOT + FileUtil.extName(file.getOriginalFilename());
        Map<String, Object> resultMap = new HashMap<>(4);
        resultMap.put("bucketName", ossProperties.getBucketName());
        resultMap.put("fileName", fileName);
        resultMap.put("original", file.getName());
        resultMap.put("url", String.format("/resources/%s/%s", ossProperties.getBucketName(), fileName));

        try {
            minioTemplate.putObject(ossProperties.getBucketName(), fileName, file.getContentType(), file.getInputStream());
            // 文件管理数据记录,收集管理追踪文件
            resultMap.put("id", fileLog(file, fileName, userId,sort));  ;
        } catch (Exception e) {
            log.error("上传失败", e);
            return null;
        }
        return resultMap;
    }

    @Override
    public LinkedList<Map<String, Object>> uploadFiles(MultipartFile[] files, String userId,int sort) {
        LinkedList<Map<String, Object>> resultList = new LinkedList<>();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file=files[i];
            String fileName = IdUtil.simpleUUID() + StrUtil.DOT + FileUtil.extName(file.getOriginalFilename());
            Map<String, Object> resultMap = new HashMap<>(4);
            resultMap.put("fileName", fileName);
            resultMap.put("original", file.getOriginalFilename());
            resultMap.put("url", String.format("%s%s", awsUrl, fileName));
            try {
                minioTemplate.putObject(ossProperties.getBucketName(), fileName, file.getContentType(), file.getInputStream());
                resultMap.put("done", "1");
                // 文件管理数据记录,收集管理追踪文件
                resultMap.put("id", fileLog(file, fileName, userId,sort+1)); ;
            } catch (Exception e) {
                resultMap.put("done", "2");
                resultMap.put("id", "0");
                log.error("上传失败", e);

            }
            resultList.add(resultMap);
        }
        return resultList;
    }
    public static String awsUrl ="https://dat3.s3.ap-northeast-1.amazonaws.com/";
    //https://dat3.s3.ap-northeast-1.amazonaws.com/55ee156efc7f4c68bbd383247da554d3.jpg
    @Override
    public LinkedList<Map<String, Object>> getFilesUrl(String userId  ) {
        LinkedList<Map<String, Object>> resultList = new LinkedList<>();
        List<SysFile> list = this.list(Wrappers.<SysFile>lambdaQuery()
                .eq(SysFile::getCreateUser, userId)
                .ne(SysFile::getDelFlag,"1")
                .groupBy(SysFile::getSort));
        for (int i = 0; i < list.size(); i++) {
            SysFile sysFile = list.get(i);
            Map<String, Object> resultMap = new HashMap<>(4);
            resultMap.put("fileName", sysFile.getFileName());
            resultMap.put("original", sysFile.getOriginal() );
            resultMap.put("sort", ""+sysFile.getSort() );
            resultMap.put("id", ""+sysFile.getId() );
            resultMap.put("url", String.format("%s%s", awsUrl, sysFile.getFileName()));
            resultList.add(resultMap);
        }
        return resultList;
    }
    @Override
    public Boolean modifyFiles(List<SysFile> files) {
        return this.updateBatchById(files);
    }

    @Override
    public Map<String, Object> uploadFile(InputStream inputStream, String OriginalFilename, String fileName, String contentType, String userId) {
        String fileUUIDName = IdUtil.simpleUUID() + StrUtil.DOT + FileUtil.extName(OriginalFilename);
        Map<String, Object> resultMap = new HashMap<>(4);
        resultMap.put("bucketName", ossProperties.getBucketName());
        resultMap.put("fileName", fileUUIDName);
        resultMap.put("original", fileName);
        resultMap.put("url", String.format("/resources/%s/%s", ossProperties.getBucketName(), fileUUIDName));
        //private void fileLog(String fileName, String originalFilename,long size, String userId) {

        try {
            int available = inputStream.available();
            minioTemplate.putObject(ossProperties.getBucketName(), fileUUIDName, contentType, inputStream);
            // 文件管理数据记录,收集管理追踪文件
            fileLog(fileUUIDName, OriginalFilename, available, userId);
        } catch (Exception e) {
            log.error("上传失败", e);
            return null;
        }
        return resultMap;
    }

    /**
     * 读取文件
     *
     * @param bucket
     * @param fileName
     * @param response
     */
    @Override
    public void getFile(String bucket, String fileName, HttpServletResponse response) {
        File res = new File(ChatConfig.RESOURCES_PATH + bucket + "/" + fileName);
        try {
            if (res.exists()) {
                FileInputStream fileInputStream = new FileInputStream(res);
                response.setContentType(MediaType.IMAGE_JPEG_VALUE);
                IoUtil.copy(fileInputStream, response.getOutputStream());
            }
        } catch (Exception e) {
            log.error("文件读取异常: {}", e.getLocalizedMessage());
        }

        try (S3Object s3Object = minioTemplate.getObject(bucket, fileName)) {

            InputStream inputStream = s3Object.getObjectContent();
            response.setContentType(MediaType.IMAGE_JPEG_VALUE);
            response.addHeader("Cache-Control", "max-age=60");
            //IoUtil.copy(inputStream, response.getOutputStream());
            IoUtil.copy(inputStream, Files.newOutputStream(res.toPath()));
            FileInputStream fileInputStream = new FileInputStream(res);
            IoUtil.copy(fileInputStream, response.getOutputStream());

        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }


    public Map getFiles(List<Integer> ids) {
        List<String> collect = ids.stream().map(s -> "DAT3 Invitation Code#" + s).collect(Collectors.toList());
        List<SysFile> list = this.list(Wrappers.<SysFile>lambdaQuery().in(SysFile::getFileName, collect));
        for (SysFile sysFile : list) {
            this.getFileUrl("dat3", sysFile.getFileName());
        }

        return null;
    }

    /**
     * 删除文件
     *
     * @param id
     * @return
     */
    @Override
    @SneakyThrows
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFile(Long id, String fileName) {
        SysFile file = new SysFile();
        if (id == null) {
            file = getOne(new LambdaQueryWrapper<SysFile>().eq(SysFile::getFileName, fileName).last("limit 1"));
        } else {
            file = this.getById(id);
        }
        if (file != null) {
            //minioTemplate.removeObject(ossProperties.getBucketName(), file.getFileName());
            file.setDelFlag("1");
            return this.updateById( file);
        }


        return false;
    }

    @Override
    public String getFileUrl(String fileName) {
        SysFile sysFile = getOne(new LambdaQueryWrapper<SysFile>().eq(SysFile::getFileName, fileName).last("limit 1"));
        return minioTemplate.getObjectURL(sysFile.getBucketName(), sysFile.getFileName(), Duration.ofDays(3));
    }



    public String getFileUrl(String b, String fileName) {
        return minioTemplate.getObjectURL(b, fileName);
    }

    /**
     * 文件管理数据记录,收集管理追踪文件
     *
     * @param file     上传文件格式
     * @param fileName 文件名
     */
    private int fileLog(MultipartFile file, String fileName, String userId,Integer sort) {
        SysFile sysFile = new SysFile();
        sysFile.setFileName(fileName);
        sysFile.setOriginal(file.getOriginalFilename());
        sysFile.setFileSize(file.getSize());
        sysFile.setType(FileUtil.extName(file.getOriginalFilename()));
        sysFile.setBucketName(ossProperties.getBucketName());
        sysFile.setCreateUser(userId);
        sysFile.setSort(sort);
        sysFile.setDelFlag("0");
        sysFile.setCreateTime(LocalDateTime.now().plusHours(8));
        int insert = this.baseMapper.insert(sysFile);
        if (insert>0) {
            return Math.toIntExact(sysFile.getId());
        }
        return 0;
    }

    private void fileLog(String fileName, String originalFilename, long size, String userId) {
        SysFile sysFile = new SysFile();
        sysFile.setFileName(fileName);
        sysFile.setOriginal(originalFilename);
        sysFile.setFileSize(size);
        sysFile.setType(FileUtil.extName(originalFilename));
        sysFile.setBucketName(ossProperties.getBucketName());
        sysFile.setCreateUser(userId);
        this.save(sysFile);
    }

}
