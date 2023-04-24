package com.chat.utils;


import com.chat.common.SecurityConstant;
import com.chat.common.TokenConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.Map;

/**
 * Jwt工具
 */
public class JwtUtils {
    /**
     * token密钥
     */
    public static String secret = TokenConstant.SECRET;

    /**
     * 创建Token
     */
    public static String CreateToken(Map<String, Object> claims ,long expiration) {
        String token = Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
        return token;
    }

    /**
     * 解析token
     */
    public static Claims parseToken(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    /**
     * 根据令牌获取用户标识
     *
     * @param claims 身份信息
     * @return 用户ID
     */
    public static String getUserKey(Claims claims) {
        return getValue(claims, SecurityConstant.USER_ID);
    }

    public static String getUserKey(String token) {
        Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        return getValue(claims, SecurityConstant.USER_ID);
    }
    public static String getUser(String token,String key) {
        Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        return getValue(claims, key);
    }
    public static String getUserAdders(String token) {
        Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        return getValue(claims, SecurityConstant.ADDRESS);
    }

    public static String getUserType(Claims claims) {
        return getValue(claims, SecurityConstant.USER_TYPE);
    }
    public static String getUserAccount(Claims claims) {
        return getValue(claims, SecurityConstant.USER_ACCOUNT);
    } public static String getUserId(Claims claims) {
        return getValue(claims, SecurityConstant.USER_ID);
    }

    /**
     * 根据身份信息获取键值
     *
     * @param claims 身份信息
     * @param key    键
     * @return 值
     */
    public static String getValue(Claims claims, String key) {
        return DataConvert.toStr(claims.get(key), "");
    }




}
