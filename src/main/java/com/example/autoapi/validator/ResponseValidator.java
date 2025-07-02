package com.example.autoapi.validator;

import com.example.autoapi.utils.ResponseDataStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Pattern;

public class ResponseValidator {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 校验状态码
     */
    public static void validateStatusCode(int actual, int expected) {
        if (actual != expected) {
            throw new AssertionError("❌ 状态码不匹配，预期：" + expected + "，实际：" + actual);
        }
    }

    /**
     * 校验字段内容（支持 not_null、regex:、contains:、具体值）
     */
    public static void validateJsonField(String json, String jsonPath, String expectedValue) {
        try {
            JsonNode node = getNode(json, jsonPath);
            String actual = node.isNull() ? null : node.asText();

            if ("not_null".equalsIgnoreCase(expectedValue)) {
                if (actual == null || actual.isEmpty()) {
                    throw new AssertionError("❌ 字段 [" + jsonPath + "] 预期为非空，但实际为空");
                }
            } else if (expectedValue.startsWith("regex:")) {
                String pattern = expectedValue.substring(6);
                if (actual == null || !Pattern.matches(pattern, actual)) {
                    throw new AssertionError("❌ 字段 [" + jsonPath + "] 不匹配正则: " + pattern);
                }
            } else if (expectedValue.startsWith("contains:")) {
                String part = expectedValue.substring(9);
                if (actual == null || !actual.contains(part)) {
                    throw new AssertionError("❌ 字段 [" + jsonPath + "] 不包含: " + part);
                }
            } else {
                if (!expectedValue.equals(actual)) {
                    throw new AssertionError("❌ 字段 [" + jsonPath + "] 不匹配，预期: " + expectedValue + "，实际: " + actual);
                }
            }

        } catch (Exception e) {
            throw new AssertionError("❌ 字段验证失败：" + jsonPath, e);
        }
    }

    /**
     * 多字段校验
     */
    public static void validateMultipleJsonFields(String json, String paths, String values) {
        String[] pathArray = paths.split(";");
        String[] valueArray = values.split(";");

        if (pathArray.length != valueArray.length) {
            throw new AssertionError("❌ 校验路径和期望值数量不一致");
        }

        for (int i = 0; i < pathArray.length; i++) {
            validateJsonField(json, pathArray[i].trim(), valueArray[i].trim());
        }
    }

    /**
     * 判断字段是否存在
     */
    public static void assertFieldExists(String json, String jsonPath) {
        getNode(json, jsonPath); // 会抛出异常表示不存在
    }

    /**
     * 从 JSON 提取字段并保存至 ResponseDataStore
     */
    public static void extractJsonField(String json, String jsonPath, String storeKey) {
        try {
            JsonNode node = getNode(json, jsonPath);
            ResponseDataStore.put(storeKey, node.isNull() ? "" : node.asText());
        } catch (Exception e) {
            throw new AssertionError("❌ 提取字段失败: " + jsonPath, e);
        }
    }

    /**
     * 内部方法：支持嵌套对象 & 数组路径
     * 如 jsonPath: "Orderdetails[0].ProductName"
     */
    private static JsonNode getNode(String json, String jsonPath) {
        try {
            JsonNode current = mapper.readTree(json);
            String[] segments = jsonPath.split("\\.");

            for (String segment : segments) {
                if (segment.contains("[") && segment.endsWith("]")) {
                    String field = segment.substring(0, segment.indexOf("["));
                    int index = Integer.parseInt(segment.substring(segment.indexOf("[") + 1, segment.length() - 1));

                    current = current.path(field);
                    if (!current.isArray() || index >= current.size()) {
                        throw new AssertionError("❌ 路径错误，数组字段不存在或索引越界: " + segment);
                    }
                    current = current.get(index);
                } else {
                    current = current.path(segment);
                    if (current.isMissingNode()) {
                        throw new AssertionError("❌ 路径不存在: " + segment + "（完整路径：" + jsonPath + "）");
                    }
                }
            }

            return current;

        } catch (Exception e) {
            throw new AssertionError("❌ 解析 JSON 路径失败: " + jsonPath, e);
        }
    }
}

