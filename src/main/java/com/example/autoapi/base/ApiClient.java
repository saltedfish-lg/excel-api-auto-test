package com.example.autoapi.base;

import com.example.autoapi.auth.TokenManager;
import com.example.autoapi.utils.HeaderUtil;
import com.example.autoapi.utils.ParamResolver;
import com.example.autoapi.utils.ResponseDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class ApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);

    /**
     * 通用接口执行方法：解析 headersRaw，处理 token，自动重试
     */
    public static String execute(String method, String url, String body, String headersRaw) {
        String resolvedUrl = ParamResolver.resolveWithStore(url);
        String resolvedBody = ParamResolver.resolveWithStore(body);
        String resolvedHeadersRaw = ParamResolver.resolveWithStore(headersRaw);

        Map<String, String> headers = HeaderUtil.parse(resolvedHeadersRaw);

        // 自动附加 token（除非已设置 Authorization）
        String token = ResponseDataStore.get("login_token");
        if (token != null && !headers.containsKey("Authorization")) {
            headers.put("Authorization", token);
        }
//        headers.put("Content-Type", "application/json");
        String response = sendRequest(method, resolvedUrl, resolvedBody, headers);

        if (isTokenInvalid(response)) {
            TokenManager.refreshToken();
            headers.put("Authorization", ResponseDataStore.get("login_token"));
            response = sendRequest(method, resolvedUrl, ParamResolver.resolveWithStore(body), headers);
        }

        return response;
    }

    /**
     * 实际请求发送逻辑
     */
    private static String sendRequest(String method, String url, String body, Map<String, String> headers) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10));

            // 添加 headers
            if (headers != null) {
                headers.forEach(builder::header);
            }

            // 构造请求方法
            switch (method.toUpperCase()) {
                case "GET" -> builder.GET();
                case "DELETE" -> builder.DELETE();
                case "POST", "PUT", "PATCH" -> {
                    HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.ofString(
                            body != null ? body : "", StandardCharsets.UTF_8);
                    builder.method(method.toUpperCase(), publisher);
                }
                default -> throw new IllegalArgumentException("❌ 不支持的请求方法: " + method);
            }

            HttpRequest request = builder.build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            System.out.println(response.body());
            return response.body();

        } catch (Exception e) {
            throw new RuntimeException("❌ 接口请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 判断响应中是否含有 token 失效信息
     */
    private static boolean isTokenInvalid(String response) {
        return response.contains("token失效") || response.contains("\"code\":401");
    }

    /**
     * 构建 Content-Type 为 application/json 的默认请求头
     */
    public static Map<String, String> jsonHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    public static Map<String, String> withTokenFromStore() {
        Map<String, String> headers = jsonHeader();
        String token = ResponseDataStore.get("login_token");
        if (token != null) {
            headers.put("Authorization", token);
        }
        return headers;
    }
}
