package com.chat.config.filter;

import com.alibaba.fastjson.JSON;
import com.chat.common.R;
import com.chat.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.util.NestedServletException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
@WebFilter(urlPatterns = "/*", filterName = "CrossFilter")
public class CorssFilter extends OncePerRequestFilter {

    /**
     * @Method doFilterInternal ，跨域问题解决
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String originHeader = request.getHeader("Origin");
//        if(!StringUtils.isEmpty(originHeader)) {
//            response.setHeader("Access-Control-Allow-Origin", "*");
//        }
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT, DELETE");
        response.addHeader("Access-Control-Max-Age", "86400");//30 min
        response.addHeader("Access-Control-Allow-Credentials", "true");
        response.addHeader("Access-Control-Allow-Headers", "Origin, No-Cache, X-Requested-With, If-Modified-Since, Pragma, Last-Modified, Cache-Control, Expires, Content-Type, X-E4M-With, withcredentials, token,lang");
        response.addHeader("XDomainRequestAllowed", "1");

        response.addHeader("timestamp", "" + System.currentTimeMillis());
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {

            assert (e instanceof NestedServletException) : "" + e.fillInStackTrace();
            //文件上传太大
            if (((NestedServletException) e).getRootCause() instanceof MaxUploadSizeExceededException) {
                sentError(request, response, R.custom(ResultCode.FAILED.getCode(), "The portrait file exceeds its maximum permitted size of 1m"));
            } else {
                e.printStackTrace();
            }
        }

    }

    private void sentError(HttpServletRequest request, HttpServletResponse response, R r) throws IOException {


        if (response.containsHeader("Access-Control-Allow-Origin")) {
            String header = response.getHeader("Access-Control-Allow-Origin");
            if (!"*".equals(header)) {
                response.setHeader("Access-Control-Allow-Origin", "*");
            }
        }else {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }

        if (!response.containsHeader("Access-Control-Allow-Headers")) {
            response.addHeader("Access-Control-Allow-Headers", "Origin, No-Cache, X-Requested-With, If-Modified-Since, Pragma, Last-Modified, Cache-Control, Expires, Content-Type, X-E4M-With, withcredentials, token,lang");
        }

        response.addHeader("Access-Control-Max-Age", "86400");//30 min
        response.addHeader("Access-Control-Allow-Credentials", "true");
        response.addHeader("XDomainRequestAllowed", "1");
        response.addHeader("timestamp", "" + System.currentTimeMillis());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().println(JSON.toJSONString(r));
        response.getWriter().flush();

    }
}
