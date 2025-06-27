package com.example.autoapi.notifier;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeComNotifier {

    private static final String WEBHOOK_URL = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY";

    public static void sendText(String content) {
        try {
            URL url = new URL(WEBHOOK_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            String payload = String.format("{\"msgtype\":\"text\",\"text\":{\"content\":\"%s\"}}", content);

            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8")) {
                writer.write(payload);
            }

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed to send notification");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
