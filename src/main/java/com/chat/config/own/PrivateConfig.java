package com.chat.config.own;

import java.util.HashMap;
import java.util.Map;

public class PrivateConfig {
    public static final String DAT3 = "SOME_PUBLIC_KEY";
    public static final String DAT3_NFT = "SOME_PUBLIC_KEY";
    public static final String TR_KEY = "SOME_PUBLIC_KEY";

    public static Map<String, String> addressPrivateKey = new HashMap<>();


    public static String EMS_APP_KEY = "APP_KEY#dat3";
    public static String EMS_CLIENT_ID = "SOME_CLIENT_ID";
    public static String EMS_CLIENT_SECRET = "SOME_CLIENT_SECRET";

    public static String AGORA_APP_ID = "AGORA_APP_ID";
    public static String AGORA_APP_CERT = "AGORA_APP_CERT";
    public static String AGORA_API_KEY = "AGORA_API_KEY";
    public static String AGORA_API_SECRET = "AGORA_API_SECRET";

    static {
        addressPrivateKey.put("SOME_PUBLIC_KEY", "SOME_PRIVATE_KEY");
    }
}
