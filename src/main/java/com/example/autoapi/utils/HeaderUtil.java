package com.example.autoapi.utils;

import java.util.HashMap;
import java.util.Map;

public class HeaderUtil {

    /**
     * 将 headersRaw 字符串解析成 Map
     * 格式: Authorization=xxx; Content-Type=application/json
     */
    public static Map<String, String> parse(String headersRaw) {
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
