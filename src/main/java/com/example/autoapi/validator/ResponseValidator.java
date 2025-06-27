package com.example.autoapi.validator;

import com.example.autoapi.utils.ResponseDataStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Pattern;

public class ResponseValidator {

    private static final ObjectMapper mapper = new ObjectMapper();

    // 状态码校验
    public static void validateStatusCode(int actual, int expected) {
        if (actual != expected) {
            throw new AssertionError("状态码不匹配，预期：" + expected + "，实际：" + actual);
        }
    }

    // 单字段校验（支持 not_null、contains:xxx、regex:xxx、直接比较）
    public static void validateJsonField(String json, String jsonPath, String expectedValue) {
        try {
            JsonNode root = mapper.readTree(json);
            String[] keys = jsonPath.split("\\.");
            JsonNode node = root;
            for (String key : keys) {
                node = node.path(key);
            }

            if ("not_null".equalsIgnoreCase(expectedValue)) {
                if (node.isNull() || node.asText().isEmpty()) {
                    throw new AssertionError("字段 " + jsonPath + " 预期为非空，但实际为空");
                }
            } else if (expectedValue.startsWith("regex:")) {
                String regex = expectedValue.substring(6);
                if (!Pattern.matches(regex, node.asText())) {
                    throw new AssertionError("字段 " + jsonPath + " 不匹配正则: " + regex);
                }
            } else if (expectedValue.startsWith("contains:")) {
                String part = expectedValue.substring(9);
                if (!node.asText().contains(part)) {
                    throw new AssertionError("字段 " + jsonPath + " 不包含: " + part);
                }
            } else {
                if (!node.asText().equals(expectedValue)) {
                    throw new AssertionError("字段 " + jsonPath + " 不匹配，预期: " + expectedValue + "，实际: " + node.asText());
                }
            }
        } catch (Exception e) {
            throw new AssertionError("字段验证失败：" + jsonPath, e);
        }
    }

    // 多字段批量校验（字段和预期值用 ; 分隔）
    public static void validateMultipleJsonFields(String json, String paths, String values) {
        String[] pathArray = paths.split(";");
        String[] valueArray = values.split(";");

        if (pathArray.length != valueArray.length) {
            throw new AssertionError("checkFields 和 expectedValues 长度不一致");
        }

        for (int i = 0; i < pathArray.length; i++) {
            validateJsonField(json, pathArray[i].trim(), valueArray[i].trim());
        }
    }

    // 判断字段是否存在
    public static void assertFieldExists(String json, String jsonPath) {
        try {
            JsonNode root = mapper.readTree(json);
            String[] keys = jsonPath.split("\\.");
            JsonNode node = root;
            for (String key : keys) {
                if (!node.has(key)) {
                    throw new AssertionError("字段不存在：" + key + " in path " + jsonPath);
                }
                node = node.get(key);
            }
        } catch (Exception e) {
            throw new AssertionError("字段存在性检查失败：" + jsonPath, e);
        }
    }

    // 字段提取并保存到变量池
    public static void extractJsonField(String json, String jsonPath, String storeKey) {
        try {
            JsonNode root = mapper.readTree(json);
            String[] keys = jsonPath.split("\\.");
            JsonNode node = root;
            for (String key : keys) {
                node = node.path(key);
            }
            String value = node.asText();
            ResponseDataStore.put(storeKey, value);
        } catch (Exception e) {
            throw new AssertionError("字段提取失败：" + jsonPath, e);
        }
    }
}
