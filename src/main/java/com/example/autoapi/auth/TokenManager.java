package com.example.autoapi.auth;

import com.example.autoapi.base.ApiClient;
import com.example.autoapi.config.EnvConfig;
import com.example.autoapi.utils.ParamResolver;
import com.example.autoapi.utils.ResponseDataStore;
import com.example.autoapi.validator.ResponseValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenManager {

    private static final Logger logger = LoggerFactory.getLogger(TokenManager.class);

    /**
     * 自动刷新 token（接口 401 时调用）
     */
    public static void refreshToken() {
        try {
            String baseUrl = EnvConfig.get("base.url");
            String url = baseUrl + "/meio/Users/PasswordLogin";

            String user = EnvConfig.get("login.user");
            String pass = EnvConfig.get("login.pass");
            String body = String.format("{\"account\":\"%s\",\"password\":\"%s\"}", user, pass);

            String resolvedUrl = ParamResolver.resolve(url);
            String resolvedBody = ParamResolver.resolve(body);

            String response = ApiClient.post(resolvedUrl, resolvedBody, false);

            ResponseValidator.validateJsonField(response, "data.AccessToken", "not_null");
            ResponseValidator.extractJsonField(response, "data.AccessToken", "login_token");

            logger.info("🔄 token refreshed: {}", ResponseDataStore.get("login_token"));

        } catch (Exception e) {
            logger.error("❌ Token刷新失败: {}", e.getMessage(), e);
            throw new RuntimeException("Token刷新失败", e);
        }
    }

    /**
     * 启动前登录一次获取 token，可在 @BeforeSuite 调用
     */
    public static void loginAndStoreToken() {
        try {
            String baseUrl = EnvConfig.get("base.url");
            String url = baseUrl + "/meio/Users/PasswordLogin";

            String user = EnvConfig.get("login.user");
            String pass = EnvConfig.get("login.pass");
            String body = String.format("{\"account\":\"%s\",\"password\":\"%s\"}", user, pass);

            String resolvedUrl = ParamResolver.resolve(url);
            String resolvedBody = ParamResolver.resolve(body);

            String response = ApiClient.post(resolvedUrl, resolvedBody, false);
            System.out.println(response);

            ResponseValidator.validateJsonField(response, "data.AccessToken", "not_null");
            ResponseValidator.extractJsonField(response, "data.AccessToken", "login_token");
            logger.info("✅ 登录成功，token: {}", ResponseDataStore.get("login_token"));

        } catch (Exception e) {
            logger.error("🚨 登录失败: {}", e.getMessage());
            throw new RuntimeException("登录失败，无法获取 token", e);
        }
    }
}
