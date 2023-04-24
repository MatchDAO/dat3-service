package com.chat.config.filter;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
@Component
@Slf4j
public class MyLocaleResolver implements LocaleResolver {

    @Override
    public Locale resolveLocale(HttpServletRequest request) {

        try {
            String paramLanguage = request.getParameter("lang");
            if(!StringUtils.isEmpty(paramLanguage)){
                String[] splits = paramLanguage.split("_");
                if(splits[0]==paramLanguage){
                    splits= paramLanguage.split("-");
                }
                return new Locale(splits[0], splits[1]);
            }else if(!StrUtil.isEmpty(request.getHeader("lang"))){

                String acceptLanguage = request.getHeader("lang").split(",")[0];
                String[] splits = acceptLanguage.split("_");
                if(splits[0]==paramLanguage){
                    splits= paramLanguage.split("-");
                }
                return new Locale(splits[0], splits[1]);
            }else {
                return new Locale("en", "US");
            }
        }catch (Exception e){
            log.info("MyLocaleResolver "+e.fillInStackTrace());
           return    new Locale("en", "US");
        }

    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {

    }

}