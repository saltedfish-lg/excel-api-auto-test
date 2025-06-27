package com.example.autoapi.config;

import java.io.InputStream;
import java.util.Properties;

public class GlobalConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = FrameworkConfig.class.getClassLoader().getResourceAsStream("config/config.properties")) {
            if (in == null) throw new RuntimeException("❌ config.properties 未找到");
            props.load(in);
        } catch (Exception e) {
            throw new RuntimeException("❌ 加载配置失败: " + e.getMessage());
        }
//        try {
//            props.load(new FileInputStream("src/main/resources/config.properties"));
//        } catch (Exception e) {
//            throw new RuntimeException("❌ 加载配置失败: " + e.getMessage());
//        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
