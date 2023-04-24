package com.chat.entity;

import lombok.Data;

@Data
public class MailRequest {
    /**
     * 接收人
     */
    private String sendTo;

    /**
     *  邮件主题
     */
    private String subject;

    /**
     *  邮件内容
     */
    private String text;

    /**
     *  附件路径
     */
    private String filePath;

}
