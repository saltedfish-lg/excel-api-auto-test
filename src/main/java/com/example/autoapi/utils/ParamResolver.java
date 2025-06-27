package com.example.autoapi.utils;

import com.example.autoapi.config.EnvConfig;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParamResolver {

    private static final Pattern pattern = Pattern.compile("\\$\\{(.*?)}");

    /**
     * 替换 ${key} 占位符，仅使用配置文件中的变量
     */
    public static String resolve(String input) {
        return replaceParams(input, null);
    }

    /**
     * 替换 ${key} 占位符，优先从缓存池中取值（如 login_token 等）
     */
    public static String resolveWithStore(String input) {
        return replaceParams(input, ResponseDataStore.getAll());
    }

    private static String replaceParams(String input, Map<String, String> store) {
        if (input == null || input.isBlank()) return input;

        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = null;

            if (store != null && store.containsKey(key)) {
                value = store.get(key);
            } else {
                value = EnvConfig.get(key); // fallback 到配置文件
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : ""));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }
}
