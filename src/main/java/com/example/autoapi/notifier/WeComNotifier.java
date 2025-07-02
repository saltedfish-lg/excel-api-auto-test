package com.example.autoapi.notifier;

import com.example.autoapi.config.EnvConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeComNotifier {

    private static final Logger logger = LoggerFactory.getLogger(WeComNotifier.class);
    private static final String WEBHOOK_URL = EnvConfig.get("wecom.webhook");

    // 创建 ObjectMapper 实例
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送文本消息到企业微信
     * @param content 消息内容
     */
    public static void sendText(String content) {
        try {
            // 构建文本消息的 JSON 格式
            String payload = "{"
                    + "\"msgtype\": \"text\","
                    + "\"text\": {"
                    + "\"content\": \"" + content + "\""
                    + "}";

            sendRequest(payload);
        } catch (Exception e) {
            logger.error("❌ 企业微信发送文本消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送Markdown格式消息到企业微信
     * @param markdownContent Markdown 格式的消息内容
     */
    public static void sendMarkdown(String markdownContent) {
        try {
            // 构建Markdown消息的 JSON 格式
            Map<String, Object> payload = new HashMap<>();
            payload.put("msgtype", "markdown");

            // 构建实际的内容
            Map<String, String> content = new HashMap<>();
            content.put("content", markdownContent);

            // 将 content 放入 markdown 键中
            payload.put("markdown", content);

            // 将 payload 转换为 JSON 格式字符串
            String payloadJson = objectMapper.writeValueAsString(payload);

            // 发送请求
            sendRequest(payloadJson);
        } catch (Exception e) {
            logger.error("❌ 企业微信发送Markdown消息失败: {}", e.getMessage(), e);
        }
    }


    /**
     * 发送 HTTP 请求到企业微信 Webhook
     * @param payload 消息内容的 JSON 格式
     */
    private static void sendRequest(String payload) {
        if (WEBHOOK_URL == null || WEBHOOK_URL.isEmpty()) {
            logger.warn("⚠️ 企业微信 webhook 未配置，跳过发送通知");
            return;
        }

        try {
            URL url = new URL(WEBHOOK_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            // 发送请求
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 获取响应码
            connection.getResponseCode();
            logger.info("📢 企业微信已发送通知");

        } catch (Exception e) {
            logger.error("❌ 企业微信通知失败: {}", e.getMessage(), e);
        }
    }
}
