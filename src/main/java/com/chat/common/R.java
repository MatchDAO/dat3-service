package com.chat.common;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用返回结果，服务端响应的数据最终都会封装成此对象
 *
 * @param <T>
 * @author Sire
 */
@Data
public class R<T> implements Serializable {
    /**
     * 编码：1成功，0和其它数字为失败
     */
    private Integer code;
    /**
     * 错误信息
     */
    private String msg;
    /**
     * 数据
     */
    private T data;
    /**
     * 动态数据
     */
    private Map map = new HashMap();

    public static <T> R<T> custom(Integer code, String msg) {
        return custom(code, msg, null);
    }

    public static <T> R<T> custom(Integer code, String msg, T object) {
        R<T> r = new R<T>();
        r.data = object;
        r.code = code;
        r.msg = msg;
        return r;
    }


    public static <T> R<T> success(String msg) {

        return success(msg,null);
    }

    public static <T> R<T> success(String msg, T object) {
        return custom( 200,  msg,  object);
    }


    public static <T> R<T> success(T object) {
        return success(null,object);
    }

    public static <T> R<T> success() {
        R<T> r = new R<T>();
        r.data = null;
        r.code = 200;
        return r;
    }

    public static <T> R<T> needValidate(String msg, T object) {
        R<T> r = new R<T>();
        r.msg = msg;
        r.data = object;
        r.code = 403;
        return r;
    }

    public static <T> R<T> error(String msg) {
        R r = new R();
        r.msg = msg;
        r.code = 500;
        return r;
    }

    public static <T> R<T> error() {
        R r = new R();
        r.msg = null;
        r.code = 500;
        return r;
    }

    public R<T> add(String key, Object value) {
        this.map.put(key, value);
        return this;
    }

}
