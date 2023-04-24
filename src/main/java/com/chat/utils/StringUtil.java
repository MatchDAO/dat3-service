package com.chat.utils;


import cn.hutool.core.util.StrUtil;
import org.springframework.util.AntPathMatcher;

import java.util.Collection;
import java.util.List;

public class StringUtil extends org.apache.commons.lang3.StringUtils {

    private static final char UNDERLINE='_';
    /**
     * 下划线 转 驼峰
     * @param param
     * @return
     */
    public static String underlineToCamel(String param){
        if (param==null||"".equals(param.trim())){
            return "";
        }
        int len=param.length();
        StringBuilder sb=new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = Character.toLowerCase(param.charAt(i));
            if (c == UNDERLINE){
                if (++i<len){
                    sb.append(Character.toUpperCase(param.charAt(i)));
                }
            }else{
                sb.append(c);
            }
        }
        return sb.toString();
    }

    //驼峰转下划线
    public static String underscoreName(String name) {
        StringBuilder result = new StringBuilder();
        if(name != null && name.length()>0){
            for (int i = 0; i < name.length(); i++) {
                String s = name.substring(i,i+1);
                if(s.equals(s.toUpperCase())&&!Character.isDigit(s.charAt(0))){
                    result.append("_");
                }
                result.append(s.toLowerCase());

            }
        }
        return result.toString();
    }

    /**
     * 判断一个Collection 是否为空 包含 list set
     *
     * @Param coll 要判断验证的对象
     * @Return true：空，false 非空
     */
    public static boolean isEmpty(Collection<?> coll) {
        return isNull(coll) || coll.isEmpty();
    }
    /**
     * 判断Object是否为NUll
     *
     * @Param object
     * @Return true 为null ；false
     */
    public static boolean isNull(Object object) {
        return object == null;
    }

    /**
     * 查找指定字符串是否匹配指定字符串列表中的任意一个字符串
     *
     * @param str        指定字符串
     * @param stringList 需要检查的字符串数组
     * @return 是否匹配
     */
    public static boolean matches(String str, List<String> stringList) {
        if (isEmpty(str) || isEmpty(stringList)) {
            return false;
        }
        for (String pattern : stringList) {
            if (isMatch(pattern, str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断url是否与规则配置:
     * ? 表示单个字符;
     * * 表示一层路径内的任意字符串，不可跨层级;
     * ** 表示任意层路径;
     *
     * @param pattern 匹配规则
     * @param url     需要匹配的url
     * @return
     */
    public static boolean isMatch(String pattern, String url) {
        AntPathMatcher matcher = new AntPathMatcher();
        return matcher.match(pattern, url);
    }


    public static String desensitized(String str,Character searchChar ,int startInclude) {
        if (StrUtil.isBlank(str)) {
            return StrUtil.EMPTY;
        }
        int index = StrUtil.indexOf(str, searchChar);
        if (index < 1) {
            index =str.length();
        }
        if(index < startInclude){
            return str;
        }
        return StrUtil.hide(str, startInclude, index);
    }
}
