package com.chat.utils;

import cn.hutool.core.util.RandomUtil;
import com.chat.config.ChatConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Component
public class UserIdUtils {
    @Resource
    private RedisUtil redisUtil;
    private static final Integer ID_LENGTH = 6;

    private final Integer data_prefix=3;
    private final Integer len= ChatConfig.USERID_LENGTH-data_prefix;
    public static String getId(){
        // 生成一个由大写字母和数字组成的6位随机字符串，并且字符串不重复
        char[] letters = {'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
                'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
                '0','1','2','3','4','5','6','7','8','9'};
        boolean[] flags = new boolean[letters.length];
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ID_LENGTH; i++) {
            int index;
            do {
                index = (int) (Math.random()*(letters.length));
            }while (flags[index]);//判断生成的字符串是否重复
            String userId = Character.toString(letters[index]);
            sb.append(userId);
            flags[index] = true;
        }
        //System.out.println(sb.toString());
        return sb.toString();
    }
    //todo id服务
    public String getNewUserCode(String key) {
        String newN3 = "";
        String lestUserCode = DataConvert.toStr(redisUtil.get("lestUserCode"),"0000000");

        LocalDateTime date = LocalDateTime.now();
        String n1 = String.valueOf(date.getYear()).substring(String.valueOf(date.getYear()).length() - 1);
        String n2 = fillStr(String.valueOf(date.getMonth().getValue()), '0', 2, Boolean.TRUE);
        String newN2 = lestUserCode.substring(1, 3);
        String n3 = lestUserCode.substring(3);
        if (!newN2.equals(n2)||Long.parseLong(n3)>99988) {
            newN3 = n1 + n2 + "1" + RandomUtil.randomString("0123456789", len - 1);
            redisUtil.set("lestUserCode",DataConvert.toLong(newN3));
        }else {
            long incr = redisUtil.incr("lestUserCode", 1L);
            newN3 = DataConvert.toStr(incr,null);
        }

        return newN3;
    }
    public static String fillStr(String str, char filledChar, int len, boolean isPre) {
        final int strLen = str.length();
        if (strLen > len) {
            return str;
        }
        String filledStr = repeat(filledChar, len - strLen);
        return isPre ? filledStr.concat(str) : str.concat(filledStr);
    }

    private static String repeat(char c, int count) {
        if (count <= 0) {
            return "";
        }
        char[] result = new char[count];
        for (int i = 0; i < count; i++) {
            result[i] = c;
        }
        return new String(result);
    }
}
