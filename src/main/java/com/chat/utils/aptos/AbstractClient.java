package com.chat.utils.aptos;

import cn.hutool.json.JSONUtil;
import com.chat.utils.aptos.request.v1.model.RequestInfo;
import com.chat.utils.aptos.request.v1.model.Transaction;
import com.chat.utils.aptos.request.v1.rpc.request.IAptosRequest;
import com.chat.utils.aptos.request.v1.rpc.request.RequestSimulateTransaction;
import com.chat.utils.aptos.request.v1.rpc.request.RequestSubmitBatchTransaction;
import com.chat.utils.aptos.request.v1.rpc.request.RequestSubmitTransaction;
import com.chat.utils.aptos.utils.Jackson;
import com.chat.utils.aptos.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.ByteString;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author liqiang
 */
@SuppressWarnings({"all"})
@Slf4j
public abstract class AbstractClient {

    static final MediaType MEDIA_TYPE_JSON_UTF8 = MediaType.parse("application/json;charset=utf-8");

    static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");

    final String host;

    final Consumer<RequestInfo> info;

    final Consumer<String> logC;

    final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

    public AbstractClient(String host, Consumer<RequestInfo> info, Consumer<String> log) {
        this.host = host;
        this.info = info;
        this.logC = log;
    }

    public <T> com.chat.utils.aptos.request.v1.model.Response<T> call(IAptosRequest request, Class<T> clazz) {
        String content = null;
        RequestInfo info = RequestInfo.builder()
                .result(true)
                .request(request)
                .build();
        com.chat.utils.aptos.request.v1.model.Response response = new com.chat.utils.aptos.request.v1.model.Response<T>();
        try {
            content = this.request(request);
            if (Objects.isNull(content) || StringUtils.isEmpty(content)) {
                response.setErrorCode("content is null");
                this.logC.accept("6:" + Jackson.toJson(response));
                return response;
            }

            if (!String.class.equals(clazz) || !content.startsWith("\"0x")) {

                Map map = Jackson.readValue(content, Map.class);
                Object message = map.get("message");
                Object errorCode = map.get("error_code");
                Object vmErrorCode = map.get("vm_error_code");
                Object vmStatus = map.get("vm_status");
                if (!Objects.isNull(errorCode) && StringUtils.isNotEmpty(errorCode.toString())) {
                    response.setMessage(Objects.isNull(message) ? "" : message.toString());
                    response.setErrorCode(Objects.isNull(errorCode) ? "" : errorCode.toString());
                    response.setVmErrorCode(Objects.isNull(vmErrorCode) ? "" : vmErrorCode.toString());
                    response.setVmStatus(Objects.isNull(vmStatus) ? "" : vmStatus.toString());

                    info.setResult(false);
                    info.setMessage(response.getMessage());
                    info.setErrorCode(response.getErrorCode());
                    info.setVmErrorCode(response.getVmErrorCode());
                    info.setVmStatus(response.getVmStatus());

                    this.logC.accept("0:" + Jackson.toJson(response));
                    this.info.accept(info);

                    return response;
                }
            }

            response.setData(Jackson.readValue(content, clazz));
        } catch (Exception e) {
            e.fillInStackTrace();
            response.setMessage(e.getMessage());
            response.setErrorCode(e.getMessage());
            response.setVmErrorCode(e.getMessage());

            info.setResult(false);
            info.setMessage(response.getMessage());
            info.setErrorCode(response.getErrorCode());
            info.setVmErrorCode(response.getVmErrorCode());

            this.logC.accept("1:" + Jackson.toJson(response));
            this.logC.accept("2:" + content);
        }

        this.info.accept(info);

        return response;
    }
    public <T> com.chat.utils.aptos.request.v1.model.Response<T> viewCall(IAptosRequest request ) {
        String content = null;
        RequestInfo info = RequestInfo.builder()
                .result(true)
                .request(request)
                .build();
        com.chat.utils.aptos.request.v1.model.Response response = new com.chat.utils.aptos.request.v1.model.Response<T>();
        try {
            content = this.request(request);
            if (Objects.isNull(content) || StringUtils.isEmpty(content)) {
                response.setErrorCode("content is null");
                this.logC.accept("6:" + Jackson.toJson(response));
                return response;
            }
            if (JSONUtil.isTypeJSON(content)) {
                if (JSONUtil.isTypeJSONArray(content)) {
                    response.setData(JSONUtil.parseArray(content));
                }else {
                    response.setData(JSONUtil.parseObj(content));
                }
            }


        } catch (Exception e) {
            e.fillInStackTrace();
            response.setMessage(e.getMessage());
            response.setErrorCode(e.getMessage());
            response.setVmErrorCode(e.getMessage());

            info.setResult(false);
            info.setMessage(response.getMessage());
            info.setErrorCode(response.getErrorCode());
            info.setVmErrorCode(response.getVmErrorCode());

            this.logC.accept("1:" + Jackson.toJson(response));
            this.logC.accept("2:" + content);
        }

        this.info.accept(info);

        return response;
    }

    public <T> com.chat.utils.aptos.request.v1.model.Response<T> simulateCall(IAptosRequest request ) {
        String content = null;
        RequestInfo info = RequestInfo.builder()
                .result(true)
                .request(request)
                .build();
        com.chat.utils.aptos.request.v1.model.Response response = new com.chat.utils.aptos.request.v1.model.Response<T>();
        try {
            content = this.request(request);
            if (Objects.isNull(content) || StringUtils.isEmpty(content)) {
                response.setErrorCode("content is null");
                this.logC.accept("6:" + Jackson.toJson(response));
                return response;
            }
            if (JSONUtil.isTypeJSON(content)) {
                if (JSONUtil.isTypeJSONArray(content)) {
                    response.setData(JSONUtil.parseArray(content).get(0, Transaction.class));
                }else {
                    response.setData(JSONUtil.parseObj(content));
                }
            }


        } catch (Exception e) {
            e.fillInStackTrace();
            response.setMessage(e.getMessage());
            response.setErrorCode(e.getMessage());
            response.setVmErrorCode(e.getMessage());

            info.setResult(false);
            info.setMessage(response.getMessage());
            info.setErrorCode(response.getErrorCode());
            info.setVmErrorCode(response.getVmErrorCode());

            this.logC.accept("1:" + Jackson.toJson(response));
            this.logC.accept("2:" + content);
        }

        this.info.accept(info);

        return response;
    }
    public <T> com.chat.utils.aptos.request.v1.model.Response<List<T>> callList(IAptosRequest request, Function<String, List<T>> function) {
        String content = null;
        RequestInfo info = RequestInfo.builder()
                .result(true)
                .request(request)
                .build();
        com.chat.utils.aptos.request.v1.model.Response response = new com.chat.utils.aptos.request.v1.model.Response<List<T>>();
        try {
            content = this.request(request);
            if (Objects.isNull(content) || StringUtils.isEmpty(content)) {
                response.setErrorCode("content is null");
                this.logC.accept("7:" + Jackson.toJson(response));
                return response;
            }

            if (!content.startsWith("[")) {
                Map map = Jackson.readValue(content, Map.class);
                if (!Objects.isNull(map)) {
                    Object message = map.get("message");
                    Object errorCode = map.get("error_code");
                    Object vmErrorCode = map.get("vm_error_code");
                    Object vmStatus = map.get("vm_status");
                    if (!Objects.isNull(errorCode) && StringUtils.isNotEmpty(errorCode.toString())) {
                        response.setMessage(Objects.isNull(message) ? "" : message.toString());
                        response.setErrorCode(Objects.isNull(errorCode) ? "" : errorCode.toString());
                        response.setVmErrorCode(Objects.isNull(vmErrorCode) ? "" : vmErrorCode.toString());
                        response.setVmStatus(Objects.isNull(vmStatus) ? "" : vmStatus.toString());

                        info.setResult(false);
                        info.setMessage(response.getMessage());
                        info.setErrorCode(response.getErrorCode());
                        info.setVmErrorCode(response.getVmErrorCode());
                        info.setVmStatus(response.getVmStatus());

                        this.logC.accept("3:" + Jackson.toJson(response));
                        this.info.accept(info);

                        return response;
                    }
                }
            }

            response.setData(function.apply(content));
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            response.setErrorCode(e.getMessage());
            response.setVmErrorCode(e.getMessage());

            info.setResult(false);
            info.setMessage(response.getMessage());
            info.setErrorCode(response.getErrorCode());
            info.setVmErrorCode(response.getVmErrorCode());

            this.logC.accept("4:" + Jackson.toJson(response));
            this.logC.accept("5:" + content);
        }

        this.info.accept(info);

        return response;
    }

    Request getRequest(IAptosRequest request) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.host);
        stringBuilder.append(request.path());
        if (Objects.nonNull(request.query())) {
            stringBuilder.append("?");
            Map map = Jackson.readValue(request.query(), Map.class);
            map.forEach(new BiConsumer() {
                @Override
                public void accept(Object s, Object o) {
                    stringBuilder.append(s);
                    stringBuilder.append("=");
                    stringBuilder.append(o);
                    stringBuilder.append("&");
                }
            });
        }

        switch (request.method()) {
            case GET: {
                return new Request.Builder()
                        .get()
                        .url(stringBuilder.toString())
                        .header("Content-Type","application/json")
                        .build();
            }
            case POST: {
                if (Objects.isNull(request.body())) {
                    throw new RuntimeException("body is null");
                }

                RequestBody body;
                if (StringUtils.endsWithIgnoreCase(request.path(), RequestSubmitTransaction.PATH)
                        || StringUtils.endsWithIgnoreCase(request.path(), RequestSubmitBatchTransaction.PATH)
                        || StringUtils.endsWithIgnoreCase(request.path(), RequestSimulateTransaction.PATH)) {
                    body = RequestBody.create(MEDIA_TYPE_JSON, ByteString.encodeUtf8(Jackson.toJson(request.body())));
                } else {
                    body = RequestBody.create(Jackson.toJson(request.body()), MEDIA_TYPE_JSON_UTF8);
                }
                return new Request.Builder()
                        .post(body)
                        .header("Content-Type","application/json")
                        .url(stringBuilder.toString())
                        .build();
            }
            default: {
                break;
            }
        }

        throw new RuntimeException("unsupported method");
    }

    public String request(IAptosRequest request) throws IOException, InterruptedException {
        this.logC.accept("------------------------------------------------------------------------------------------------");
        this.logC.accept("path:" + request.path());
        this.logC.accept("parameter:" + Jackson.toJson(request));
        this.logC.accept("------------------------------------------------------------------------------------------------");

        Request request_ = this.getRequest(request);

        okhttp3.Response response = this.okHttpClient.newCall(request_).execute();
        String content = response.body().string();
        response.close();
        log.info("request"+content);
        return content;
    }

}