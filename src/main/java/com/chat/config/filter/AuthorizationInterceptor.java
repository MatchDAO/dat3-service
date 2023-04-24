package com.chat.config.filter;

import com.alibaba.fastjson.JSON;
import com.chat.cache.UserCache;
import com.chat.common.AuthToken;
import com.chat.common.R;
import com.chat.common.ResultCode;
import com.chat.config.UncheckUserProperties;
import com.chat.entity.dto.TokenDto;
import com.chat.service.TokenService;
import com.chat.utils.MessageUtils;
import com.chat.utils.RedisUtil;
import com.chat.utils.ServletUtils;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.ip2region.core.Ip2regionSearcher;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 拦截器: 校验token
 * 有 @AuthToken注解 且参数为true 会被校验
 */
@Component
@Slf4j
public class AuthorizationInterceptor implements HandlerInterceptor {
    @Resource
    private Ip2regionSearcher regionSearcher;
    @Resource
    private TokenService tokenService;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private UserCache userCache;
    @Resource
    private UncheckUserProperties uncheckUserProperties;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ipv4Current = ServletUtils.getRealIpAddress(request);
        String region = ipv4Current.split("\\.").length <= 4 ? regionSearcher.getAddress(ipv4Current) : "";
        // String ipv4 = ServletUtils.getRealIpAddress(request);
        log.info("{},ip:{},url:{}", region, ipv4Current, request.getRequestURL());

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            //类上的注解
            AuthToken targetType = method.getDeclaringClass().getAnnotation(AuthToken.class);
            //接口方法上的注解
            AuthToken targetMethod = method.getAnnotation(AuthToken.class);
            if ((targetMethod != null && targetMethod.validate())
                    || ((targetType != null && targetType.validate()))) {

                if ((targetMethod != null && !targetMethod.validate())) {
                    return true;
                }
                try {
                    String ipv4 = ServletUtils.getRealIpAddress(request);
                    String token = request.getHeader("token");
                    TokenDto check = tokenService.getTokenEntity(token);
                    if (check != null) {
                        if (!check.getToken().equals(token)) {
                            log.error("AuthorizationInterceptor:token:{},{}",check,token);
                            this.refreshToken("sso:token:" + check.getUserType() + ":" + check.getUserAccount());
                            sentError(response, R.custom(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage()));
                            return false;
                        }
                        //白名单不做ip校验
                        List<String> whites = uncheckUserProperties.getWhites();
                        if (whites.stream().anyMatch(s->s.equals(check.getUserAccount()))) {
                            userCache.userOnline(check.getUserId());
                            return true;
                        }
//                        if (check.getIpaddr() != null && !check.getIpaddr().equals(ipv4)) {
//                            this.refreshToken("sso:token:" + check.getUserType() + ":" + check.getUserAccount());
//                            log.error("AuthorizationInterceptor:token:{},{}",check,token);
//                            sentError(response, R.custom(ResultCode.UNAUTHORIZED.getCode(), "It seems like you are using a new device, please login again."));
//                            return false;
//                        }
                        userCache.userOnline(check.getUserId());
                        return true;
                    }
                } catch (Exception e) {
                    log.error(" " + e.getMessage());
                }

                log.error("AuthorizationInterceptor:token:{},{}",ServletUtils.getRealIpAddress(request),request.getRequestURI());
                sentError(response, R.custom(ResultCode.UNAUTHORIZED.getCode(), MessageUtils.getLocale("result.401")));
                return false;
            }
        }
        return true;
    }

    private void refreshToken(String tokenKey) {
        Object obj = redisUtil.get(tokenKey);
        //使当前token失效,记录上次登陆时间
        if (obj != null) {
            long expire = redisUtil.getExpire(tokenKey);
            TokenDto old = (TokenDto) obj;
            old.setExpireTime(expire);
            tokenService.createToken(old);
        }
    }

    private void sentError(HttpServletResponse response, R r) throws IOException {
        // response.setHeader("Access-Control-Allow-Origin","*");
        // response.setHeader("Cache-Control","no-cache");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().println(JSON.toJSONString(r));
        response.getWriter().flush();
    }
}
