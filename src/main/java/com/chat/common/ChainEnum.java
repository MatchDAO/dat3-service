package com.chat.common;

import com.chat.config.own.PrivateConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChainEnum {


    ETH("ETH", "ETH", "ETH", "1", "0", false, "https://etherscan.io/token/images/bnb_28_2.png", "https://etherscan.io/", "https://rpc.ankr.com/eth", ""),
    APTOS("APTOS", "APT", "APTOS", "1", "1", false, "https://explorer.aptoslabs.com/favicon.ico", "https://explorer.aptoslabs.com", PrivateConfig.APTOS_URL, ""),
   // APTOS("APTOS", "APT", "APTOS", "1", "1", false, "https://explorer.aptoslabs.com/favicon.ico", "https://explorer.aptoslabs.com/?network=devnet", "https://fullnode.testnet.aptoslabs.com", ""),

    BSC("BSC", "BNB", "ETH", "56", "0", false, "https://s2.coinmarketcap.com/static/img/coins/64x64/1027.png", "https://bscscan.com/", "https://binance.ankr.com", "");

    private final String name;
    private final String token;
    private final String compatible;
    private final String code;
    private final String chainDefault;
    private final Boolean lock;
    private final String icon;
    private final String scanUrl;
    private final String rpcUrl;
    private final String description;

    public static ChainEnum of(String name) {

        for (ChainEnum chainEnum : values()) {
            if (chainEnum.getName().equalsIgnoreCase(name)) {
                return chainEnum;
            }
        }
        return ChainEnum.ETH;

    }

    public static ChainEnum compatibleOf(String name) {
        ChainEnum of = of(name);
        for (ChainEnum chainEnum : values()) {
            if (chainEnum.getName().equalsIgnoreCase(of.getCompatible())) {
                return chainEnum;
            }
        }
        return ChainEnum.ETH;

    }

    public static ChainEnum ofChainDefault() {

        for (ChainEnum chainEnum : values()) {
            if ("1".equals(chainEnum.getChainDefault() ) ) {
                return chainEnum;
            }
        }
        return ChainEnum.ETH;

    }
}
