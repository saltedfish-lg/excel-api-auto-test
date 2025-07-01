package com.example.autoapi.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EnvConfig {

    private static final Properties properties = new Properties();

    static {
        try {
            // 1. 加载通用配置
            try (InputStream common = EnvConfig.class.getClassLoader().getResourceAsStream("config/config.properties")) {
                if (common != null) properties.load(common);
            }

            // 2. 加载环境专属配置（如 env-dev.properties）
            String env = properties.getProperty("env", "dev").trim();
            String envFileName = "config/env-" + env + ".properties";
            try (InputStream envInput = EnvConfig.class.getClassLoader().getResourceAsStream(envFileName)) {
                if (envInput != null) {
                    properties.load(envInput);
                } else {
                    throw new RuntimeException("❌ 环境配置文件未找到: " + envFileName);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("❌ 加载配置失败", e);
        }
    }

    /**
     * 获取配置值
     */
    public static String get(String key) {
        return properties.getProperty(key, "").trim();
    }

    /**
     * 获取配置值（带默认值）
     */
    public static String getOrDefault(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue).trim();
    }

    /**
     * 获取 int 类型配置（带默认值）
     */
    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
