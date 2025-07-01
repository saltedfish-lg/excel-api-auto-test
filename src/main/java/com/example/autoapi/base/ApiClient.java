package com.example.autoapi.base;

import com.example.autoapi.auth.TokenManager;
import com.example.autoapi.config.EnvConfig;
import com.example.autoapi.utils.HeaderUtil;
import com.example.autoapi.utils.ParamResolver;
import com.example.autoapi.utils.ResponseDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);
    private static final HttpClient client = HttpClient.newHttpClient();

    public static String execute(String method, String url, String body, String headersRaw) {
        String resolvedUrl = ParamResolver.resolveWithStore(url);
        String resolvedBody = ParamResolver.resolveWithStore(body);
        String resolvedHeadersRaw = ParamResolver.resolveWithStore(headersRaw);

        Map<String, String> headers = HeaderUtil.parse(resolvedHeadersRaw);

        String token = ResponseDataStore.get("login_token");
        if (token != null && !headers.containsKey("Authorization")) {
            headers.put("Authorization", token);
        }

        ApiResponse response = sendRequest(method, resolvedUrl, resolvedBody, headers);

        if (response.getStatusCode() == 404) {
            logger.error("🚫 接口不存在: [{} {}] -> {}", method, resolvedUrl, response.getBody());
            throw new RuntimeException("接口返回 404：资源不存在，请检查 URL 或服务是否上线");
        }

        if (isTokenInvalid(response)) {
            TokenManager.refreshToken();
            headers.put("Authorization", ResponseDataStore.get("login_token"));
            response = sendRequest(method, resolvedUrl, ParamResolver.resolveWithStore(body), headers);
        }

        return response.getBody();
    }

    private static ApiResponse sendRequest(String method, String url, String body, Map<String, String> headers) {
        ApiResponse result = new ApiResponse();
        long start = System.currentTimeMillis();

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(EnvConfig.getInt("timeout.ms", 5000)));

            switch (method.toUpperCase()) {
                case "GET" -> builder.GET();
                case "DELETE" -> builder.DELETE();
                case "POST", "PUT", "PATCH" ->
                        builder.method(method, HttpRequest.BodyPublishers.ofString(body == null ? "" : body, StandardCharsets.UTF_8));
                default -> throw new IllegalArgumentException("❌ 不支持的请求方法: " + method);
            }

            headers.forEach(builder::header);
            HttpResponse<String> httpResponse = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            result.setStatusCode(httpResponse.statusCode());
            result.setBody(httpResponse.body());
            result.setDurationMs(System.currentTimeMillis() - start);
            result.setHeaders(flattenHeaders(httpResponse.headers().map()));

            logger.debug("📨 响应体前200字符: {}", result.getBody().length() > 200 ? result.getBody().substring(0, 200) + "..." : result.getBody());
            return result;

        } catch (Exception e) {
            logger.error("❌ 请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("接口请求失败: " + e.getMessage(), e);
        }
    }

    private static Map<String, String> flattenHeaders(Map<String, List<String>> rawHeaders) {
        return rawHeaders.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.join(";", e.getValue())));
    }

    private static boolean isTokenInvalid(ApiResponse response) {
        String body = response.getBody();
        return body.contains("token失效") || body.contains("\"code\":401");
    }

    public static Map<String, String> jsonHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    public static Map<String, String> withTokenFromStore() {
        Map<String, String> headers = jsonHeader();
        String token = ResponseDataStore.get("login_token");
        if (token != null) headers.put("Authorization", token);
        return headers;
    }
}
