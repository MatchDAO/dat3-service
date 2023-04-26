package com.chat.cache;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.chat.entity.dto.WalletTicker;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TickerBase {

    private static TimedCache<String, BigDecimal> SYMBOLS = CacheUtil.newTimedCache(20 * 1000);
    private static long times1 = 1;
    private static OkHttpClient httpClient = new OkHttpClient().newBuilder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            //.proxy(new Proxy(Proxy.Type.HTTP,new InetSocketAddress("127.0.0.1",7890)))
            .build();

    public static WalletTicker currentSymbol(String symbol1, String symbol2) throws IOException {
        if (System.currentTimeMillis() - times1 > 15 * 1000) {
            currentSymbols();
        }
        symbol1 = symbol1.toUpperCase();
        symbol2 = symbol2.toUpperCase();
        String symbol = symbol1 + symbol2;
        WalletTicker ticker = WalletTicker.builder().symbol(symbol).symbol1(symbol1).symbol2(symbol2).build();
        BigDecimal bigDecimal = SYMBOLS.get(symbol);
        if (bigDecimal == null) {
            bigDecimal = SYMBOLS.get(symbol2 + symbol1);
            if (bigDecimal != null) {
                symbol = symbol2 + symbol1;
                ticker.setSymbol(symbol);
                ticker.setSymbol1(symbol2);
                ticker.setSymbol2(symbol1);
            } else {
                bigDecimal = BigDecimal.ZERO;
            }
        }
        ticker.setPrice(bigDecimal.toPlainString());
        return ticker;
    }

    public static void currentSymbols() throws IOException {
        if (System.currentTimeMillis() - times1 < 10 * 1000) {
            return;
        }
        try {
            currentSymbols(null);
        } catch (Exception e) {
            try {
                currentSymbols("https://api1.binance.com");
            } catch (Exception e1) {
                try {
                    currentSymbols("https://api2.binance.com");
                } catch (Exception e2) {
                    try {
                        currentSymbols("https://api3.binance.com");
                    } catch (Exception e3) {
                        throw e3;
                    }
                }
            }
        }
        times1 = System.currentTimeMillis();
    }

    // api = "https://api.binance.com/api/v3/ticker/price?symbol=ETHUSDT";
    private static void currentSymbols(String baseUrl) throws IOException {
        if (baseUrl == null) {
            baseUrl = "https://api.binance.com";
        }
        String api = baseUrl + "/api/v3/ticker/price";
        Request request = new Request.Builder()
                .get()
                .url(api).build();
        Response response = httpClient.newCall(request).execute();
        String resp = response.body().string();
        List<WalletTicker> tickers = JSONUtil.toList(resp, WalletTicker.class);
        if (!CollUtil.isEmpty(tickers)) {
            for (WalletTicker ticker : tickers) {
                SYMBOLS.put(ticker.getSymbol(), new BigDecimal(ticker.getPrice()));
            }
        }
    }


}
