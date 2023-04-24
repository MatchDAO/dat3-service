package com.chat.utils;

import cn.hutool.core.codec.Hashids;
import com.chat.config.ChatConfig;

/**
 * 根据用户id生成不重复邀请码
 **/
public class InvitationCodeUtil {
    private static final Hashids hashids = Hashids.create("south".toCharArray(), 12);

    public static String encode(Long id) {
        return hashids.encode(id);
    }
    public static Long decode(String code) {
        return hashids.decode(code)[0];
    }


    /**
     * 获取当前邀请码的索引
     */
    public static Long decodeIndex(String code) {
        String id = ""+decode(code);
        if (id.length()> ChatConfig.USERID_LENGTH) {
            String index = id.substring(ChatConfig.USERID_LENGTH);
            return Long.valueOf(index);
        }
      return 0L;
    }
    /**
     * 获取当前邀请码的索引
     */
    public static String decodeUserCode(String code) {
        String id = ""+ decode(code) ;
        if (id.length()> ChatConfig.USERID_LENGTH) {
            String index = id.substring(0,ChatConfig.USERID_LENGTH );
            return  index ;
        }
        return null;
    }



}