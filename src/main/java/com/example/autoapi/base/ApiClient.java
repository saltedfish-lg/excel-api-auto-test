package com.example.autoapi.base;

import com.example.autoapi.auth.TokenManager;
import com.example.autoapi.config.FrameworkConfig;
import com.example.autoapi.utils.ParamResolver;
import com.example.autoapi.utils.ResponseDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 核心 HTTP 客户端封装类，统一处理 GET / POST / PUT / DELETE 等请求方法
 * 支持自动附加 token、异常处理、失败重试、日志记录等功能
 */
public class ApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);

    // 使用 Java 11 内置 HttpClient
    private static final HttpClient client = HttpClient.newHttpClient();

    // ======================= 公共方法区 =======================

    /**
     * 发起 GET 请求，自动附加 token 头
     */
    public static String get(String url) {
        return sendRequest("GET", url, null, withAuthHeader());
    }

    /**
     * 发起 POST 请求，可选择是否附加 token
     */
    public static String post(String url, String body, boolean authRequired) {
        return sendRequest("POST", url, body, authRequired ? withAuthHeader() : jsonHeader());
    }

    /**
     * 发起 PUT 请求，可选择是否附加 token
     */
    public static String put(String url, String body, boolean authRequired) {
        return sendRequest("PUT", url, body, authRequired ? withAuthHeader() : jsonHeader());
    }

    /**
     * 发起 DELETE 请求，可选择是否附加 token
     */
    public static String delete(String url, boolean authRequired) {
        return sendRequest("DELETE", url, null, authRequired ? withAuthHeader() : jsonHeader());
    }

    /**
     * 发起 POST 请求，遇到 token 失效时自动刷新并重试一次
     */
    public static String postWithRetry(String url, String body) {
        String response = post(url, body, false);

        if (isTokenInvalid(response)) {
            logger.warn("⚠️ Token失效，正在自动刷新...");
            TokenManager.refreshToken();
            response = post(url, ParamResolver.resolveWithStore(body), false);
        }

        return response;
    }

    /**
     * 带缓存 Token 的 POST 请求，支持自动刷新
     */
    public static String postWithRetryAndToken(String url, String body) {
        Map<String, String> headers = new HashMap<>(withTokenFromStore());
        String response = sendRequest("POST", url, body, headers);

        if (isTokenInvalid(response)) {
            TokenManager.refreshToken();
            headers.put("Authorization", "Bearer " + ResponseDataStore.get("login_token"));
            response = sendRequest("POST", url, ParamResolver.resolveWithStore(body), headers);
        }

        return response;
    }

    /**
     * 支持手动自定义 Header 的 POST 请求
     */
    public static String postWithHeaders(String url, String body, Map<String, String> headers) {
        return sendRequest("POST", url, body, headers);
    }

    // ======================= 内部封装逻辑 =======================

    /**
     * 通用请求方法封装，支持各种 HTTP 方法
     *
     * @param method 请求方法（如 GET/POST/PUT/DELETE）
     * @param url 请求地址
     * @param body 请求体内容，可为空
     * @param headers 请求头集合
     * @return 响应内容字符串
     */
    private static String sendRequest(String method, String url, String body, Map<String, String> headers) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10)); // 设置超时

            // 构建请求方法
            switch (method.toUpperCase()) {
                case "GET" -> builder.GET();
                case "DELETE" -> builder.DELETE();
                case "POST", "PUT" -> builder.method(method, HttpRequest.BodyPublishers.ofString(body != null ? body : "", StandardCharsets.UTF_8));
                default -> throw new IllegalArgumentException("❌ 不支持的请求方法: " + method);
            }

            // 添加请求头
            headers.forEach(builder::header);

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String responseBody = response.body();

            // 输出日志并存储响应
            logger.info("✅ {} 请求成功: {}", method, url);
            logger.debug("📨 响应摘要: {}", responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody);

            // 保存最近一次响应结果供调试
            Files.writeString(Paths.get("logs/last-response.json"), responseBody, StandardCharsets.UTF_8);

            return responseBody;

        } catch (Exception e) {
            logger.error("❌ {} 请求失败: {}", method, e.getMessage(), e);
            throw new RuntimeException("HTTP请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 判断响应中是否含有 token 失效信息
     */
    private static boolean isTokenInvalid(String response) {
        return response.contains("token失效") || response.contains("\"code\":401");
    }

    // ======================= 请求头构建辅助 =======================

    /**
     * 构建默认 JSON 请求头
     */
    private static Map<String, String> jsonHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    /**
     * 构建附加 auth.token 的请求头（从 config.properties 中读取）
     */
    private static Map<String, String> withAuthHeader() {
        Map<String, String> headers = jsonHeader();
        String token = FrameworkConfig.get("auth.token");
        if (token != null && !token.isBlank()) {
            headers.put("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
        }
        return headers;
    }

    /**
     * 构建使用变量池中 login_token 的请求头（用于缓存登录状态）
     */
    private static Map<String, String> withTokenFromStore() {
        Map<String, String> headers = jsonHeader();
        String token = ResponseDataStore.get("login_token");
        if (token != null) {
            headers.put("Authorization", "Bearer " + token);
        }
        return headers;
    }
}
