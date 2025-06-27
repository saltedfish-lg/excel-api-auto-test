package com.example.autoapi.base;

import com.example.autoapi.auth.TokenManager;
import com.example.autoapi.config.EnvConfig;
import com.example.autoapi.utils.ParamResolver;
import com.example.autoapi.utils.ResponseDataStore;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ApiExecutor {

    /**
     * 主调方法：从测试类中调用，传 method, url, body, headersRaw
     */
    public static String execute(String method, String url, String body, String headersRaw) {
        //  先变量替换
        String resolvedHeadersRaw = ParamResolver.resolveWithStore(headersRaw);
        Map<String, String> headers = parseHeaders(resolvedHeadersRaw);

        //  自动追加 token（前提：未被显式设置）
        String token = ResponseDataStore.get("login_token");
        if (token != null && !headers.containsKey("Authorization")) {
            headers.put("Authorization", token);
        }

        String resolvedUrl = ParamResolver.resolveWithStore(url);
        String resolvedBody = ParamResolver.resolveWithStore(body);

        String response = send(method, resolvedUrl, resolvedBody, headers);

        // 🔁 自动重试逻辑（token失效）
        if (response.contains("token失效") || response.contains("\"code\":401")) {
            TokenManager.refreshToken();
            String refreshedToken = ResponseDataStore.get("login_token");
            headers.put("Authorization", refreshedToken);
            resolvedBody = ParamResolver.resolveWithStore(body); // 再次解析 body
            response = send(method, resolvedUrl, resolvedBody, headers);
        }

        return response;
    }


    /**
     * 实际请求发送逻辑，封装 Request 构建器
     */
    private static String send(String method, String url, String body, Map<String, String> headers) {
        try {
            Request request;
            switch (method.toUpperCase()) {
                case "POST":
                    request = Request.post(url).bodyString(body, ContentType.APPLICATION_JSON);
                    break;
                case "PUT":
                    request = Request.put(url).bodyString(body, ContentType.APPLICATION_JSON);
                    break;
                case "GET":
                    request = Request.get(url);
                    break;
                case "DELETE":
                    request = Request.delete(url);
                    break;
                case "PATCH":
                    request = Request.patch(url).bodyString(body, ContentType.APPLICATION_JSON);
                    break;
                default:
                    throw new IllegalArgumentException("❌ 不支持的请求方法: " + method);
            }

            if (headers != null) {
                headers.forEach(request::addHeader);
            }

            // ✅ 设置连接和响应超时
            int timeout = EnvConfig.getInt("timeout.ms", 5000);
            return request
                    .connectTimeout(Timeout.ofDays(timeout))
                    .responseTimeout(Timeout.ofDays(timeout))
                    .execute()
                    .returnContent()
                    .asString();

        } catch (IOException e) {
            throw new RuntimeException("❌ 接口请求失败: " + e.getMessage(), e);
        }
    }


    /**
     * 支持从 raw headers 字符串中解析为 Map
     * 格式示例: Authorization=Bearer xxx; Content-Type=application/json
     */
    private static Map<String, String> parseHeaders(String headersRaw) {
        Map<String, String> headers = new HashMap<>();
        if (headersRaw == null || headersRaw.isBlank()) return headers;

        String[] pairs = headersRaw.split(";");
        for (String pair : pairs) {
            String[] kv = pair.trim().split("=", 2);
            if (kv.length == 2) {
                headers.put(kv[0].trim(), kv[1].trim());
            }
        }
        return headers;
    }
}
