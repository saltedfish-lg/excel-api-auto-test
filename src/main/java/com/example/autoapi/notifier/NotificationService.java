package com.example.autoapi.notifier;

import com.example.autoapi.config.EnvConfig;

public class NotificationService {

    public static void notifyAllChannels(String message) {
        if (!Boolean.parseBoolean(EnvConfig.get("notify.enable"))) {
            return;
        }
        WeComNotifier.sendText(message);
        DingTalkNotifier.sendText(message);
    }
}

