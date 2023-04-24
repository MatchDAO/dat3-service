package com.chat.config;

public class ChatConfig {


    // userId Length
    public static int USERID_LENGTH = 8;

    //固定手续费
    //1美分 ->2023/01/09修改 ->0.1美元
    public static String CHAT_FEE = "0.01";


    //环信密码前缀 -->  前缀+id => pwd12345678
    public static String EM_PWD = "pwd";


    //验证失败次数
    public static int MAX_VALIDATION_FAILED_TIMES = 10;
    public static int VALIDATION_FAILED_CACHE_CAPACITY = 10000;


    public static final String RESOURCES_PATH = "/data/www/dat3_resources/";

    public static final String HOME = "https://dat3.app";
    public static final String TWITTER = "https://twitter.com/chatdat3";
    public static final String DISCORD = "discord.gg/yD447YwBve";
    public static final String EMAIL = "matchdao.web3@gmail.com";
    public static final String GITHUB = "https://github.com/MatchDAO";
}
