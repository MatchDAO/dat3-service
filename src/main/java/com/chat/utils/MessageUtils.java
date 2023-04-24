package com.chat.utils;

import cn.hutool.extra.spring.SpringUtil;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

public class MessageUtils {

    private MessageUtils() { throw new IllegalStateException("Utility class"); }

    private static final MessageSource messageSource = SpringUtil.getBean(MessageSource.class);

    public static String getLocale(String msg) {
        try {
            return messageSource.getMessage(msg, null, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            e.printStackTrace();
            return messageSource.getMessage(msg, null,new Locale("en", "US"));
        }
    }
    public static String getLocale(String msg,String ...args) {
        try {
            return messageSource.getMessage(msg, args, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return msg;
        }
    }

}