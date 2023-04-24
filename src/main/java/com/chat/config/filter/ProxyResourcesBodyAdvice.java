package com.chat.config.filter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chat.common.ProxyResources;
import com.chat.common.R;
import com.chat.utils.StringUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.ip2region.core.Ip2regionSearcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
 @ControllerAdvice
public class ProxyResourcesBodyAdvice implements ResponseBodyAdvice {
    @Resource
    private Ip2regionSearcher regionSearcher;

    @Override
    public boolean supports(MethodParameter methodParameter, Class aClass) {
        return true;
    }

    public static ArrayList<String> fields = CollUtil.toList("imageUrl", "bannerImageUrl", "pictureUrl", "price", "portrait", "collectionImage");
    public static Map<String, String> baseUrl = new HashMap<>();

    {
        baseUrl.put("https://lh3.googleusercontent.com/", "https://dune.market/static/image/googleusercontent/");
        baseUrl.put("https://openseauserdata.com/", "https://dune.market/static/image/openseauserdata/");
    }


    @SneakyThrows
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter methodParameter, MediaType mediaType, Class aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {

        try {
            String ipv4 = getRealIpAddress(serverHttpRequest);
            String region = ipv4.split("\\.").length <= 4 ? regionSearcher.getAddress(ipv4) : "";

            //region = "中国";
            if (StringUtil.isEmpty(region) || region.equals("中国台湾省") || region.equals("中国香港") || !region.contains("中国")) {
                return body;
            }
            if ( body!=null) {
                log.info("{},ip:{},url:{},body:{}", region, ipv4, serverHttpRequest.getURI() ,body);
                return body;
            }
            ProxyResources pox = methodParameter.getMethodAnnotation(ProxyResources.class);
            if (pox != null && body != null && pox.proxy() && body instanceof R) {
                R r = (R) body;
                Object value = r.getData();
                if (value != null) {
                    if (value instanceof ArrayList) {
                        List list = (List) value;
                        if (CollUtil.isEmpty(list)) {
                            return body;
                        }
                        if (list.get(0) instanceof String) {
                            for (int i = 0; i < list.size(); i++) {
                                String url = list.get(i).toString();
                                if (url != null && url.contains("lh3")) {
                                    url = url.replaceFirst("https://lh3.googleusercontent.com/", baseUrl.get("https://lh3.googleusercontent.com/"));
                                    list.set(i, url);
                                }
                            }
                            return body;
                        }
                        setProxy(list);
                        return body;
                    }
                    if (value instanceof IPage) {
                        IPage iPage = (IPage) value;
                        if (iPage.getRecords().size() < 1) {
                            return body;
                        }
                        List records = iPage.getRecords();
                        setProxy(records);
                        return body;
                    }
                    if (value instanceof Map) {
                        Map map = (Map) value;
                        //嵌套数据结构 Map(string,list)
                        if (map.containsKey("invitedPeopleRegistered")) {
                            JSONArray img = (JSONArray) map.get("invitedPeopleRegistered");
                            if (img != null && !img.isEmpty()) {
                                for (Object o : img) {
                                    JSONObject temp = (JSONObject) o;
                                    String url = CollectionUtil.isEmpty(temp) ? "" : temp.getString("portrait");
                                    String newUrl = "";
                                    if (url != null && url.contains("lh3")) {
                                        newUrl = url.replaceFirst("https://lh3.googleusercontent.com/", baseUrl.get("https://lh3.googleusercontent.com/"));
                                        temp.put("portrait", newUrl);
                                    }
                                }
                            }

                        }
                        for (String field : fields) {
                            if (map.containsKey(field)) {
                                Object img = map.get(field);
                                String url = img == null ? "" : img.toString();
                                String newUrl = "";
                                if (url != null && url.contains("lh3")) {
                                    newUrl = url.replaceFirst("https://lh3.googleusercontent.com/", baseUrl.get("https://lh3.googleusercontent.com/"));
                                    map.put(field, newUrl);
                                }
                            }
                        }
                        if (map.containsKey("data")) {
                            Object data = map.get("data");
                            if (data instanceof ArrayList) {
                                List list = (List) data;
                                if (CollUtil.isEmpty(list)) {
                                    return body;
                                }
                                setProxy(list);
                                return body;
                            }
                        }
                        return body;
                    }

                    Field[] declaredFields = value.getClass().getDeclaredFields();

                    List<Field> temp = Stream.of(declaredFields)
                            .filter(s -> fields.stream().anyMatch(a -> a.equals(s.getName())))
                            .peek(a -> a.setAccessible(true))
                            .collect(Collectors.toList());
                    for (Field field : temp) {
                        setUrl(value, field);
                    }
                }


            }
        } catch (Exception e) {
            log.error("ProxyResourcesBodyAdvice :{},{}", serverHttpRequest, e.getMessage());

            return body;
        }
        return body;
    }

    private void setProxy(List records) throws IllegalAccessException {
        Field[] declaredFields = records.get(0).getClass().getDeclaredFields();
        List<Field> temp = Stream.of(declaredFields)
                .filter(s -> fields.stream().anyMatch(a -> a.equals(s.getName())))
                .peek(a -> a.setAccessible(true))
                .collect(Collectors.toList());
        for (Object value : records) {
            for (Field field : temp) {
                setUrl(value, field);
            }
        }
    }

    private void setUrl(Object value, Field field) throws IllegalAccessException {
        Object img = field.get(value);
        String url = img == null ? "" : img.toString();
        String newUrl = "";
        if (url != null && url.contains("lh3")) {
            newUrl = url.replaceFirst("https://lh3.googleusercontent.com/", baseUrl.get("https://lh3.googleusercontent.com/"));
            field.set(value, newUrl);
        }
//        if (url != null && url.contains("opensea")) {
//            newUrl = url.replaceFirst("https://openseauserdata.com/", baseUrl.get("https://openseauserdata.com/"));
//            field.set(value, newUrl);
//        }
    }

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST = "127.0.0.1";
    private static final String SEPARATOR = ",";

    private static final String HEADER_X_FORWARDED_FOR = "x-forwarded-for";
    private static final String HEADER_PROXY_CLIENT_IP = "Proxy-Client-IP";
    private static final String HEADER_WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";

    /**
     * 获取真实客户端IP
     *
     * @param serverHttpRequest
     * @return
     */
    public static String getRealIpAddress(ServerHttpRequest serverHttpRequest) {
        String ipAddress;
        try {
            // 1.根据常见的代理服务器转发的请求ip存放协议，从请求头获取原始请求ip。值类似于203.98.182.163, 203.98.182.163
            ipAddress = serverHttpRequest.getHeaders().getFirst(HEADER_X_FORWARDED_FOR);
            if (StrUtil.isEmpty(ipAddress) || UNKNOWN.equalsIgnoreCase(ipAddress)) {
                ipAddress = serverHttpRequest.getHeaders().getFirst(HEADER_PROXY_CLIENT_IP);
            }
            if (StrUtil.isEmpty(ipAddress) || UNKNOWN.equalsIgnoreCase(ipAddress)) {
                ipAddress = serverHttpRequest.getHeaders().getFirst(HEADER_WL_PROXY_CLIENT_IP);
            }

            // 2.如果没有转发的ip，则取当前通信的请求端的ip
            if (StrUtil.isEmpty(ipAddress) || UNKNOWN.equalsIgnoreCase(ipAddress)) {
                InetSocketAddress inetSocketAddress = serverHttpRequest.getRemoteAddress();
                if (inetSocketAddress != null) {
                    ipAddress = inetSocketAddress.getAddress().getHostAddress();
                }
                // 如果是127.0.0.1，则取本地真实ip
//                if(LOCALHOST.equals(ipAddress) || "0:0:0:0:0:0:0:1".equalsIgnoreCase(ipAddress)){
                //根据网卡取本机配置的IP
//                    InetAddress inet = InetAddress.getLocalHost();
//                    ipAddress= inet.getHostAddress();
//                }
            }
            // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
            // "***.***.***.***"
            if (ipAddress != null) {
                ipAddress = ipAddress.split(SEPARATOR)[0].trim();
            }
        } catch (Exception e) {
            log.error("解析请求IP失败", e.getMessage());
            ipAddress = "";
        }
        return ipAddress == null ? "" : ipAddress;
    }


}
