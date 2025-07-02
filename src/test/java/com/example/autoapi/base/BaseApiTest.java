package com.example.autoapi.base;

import com.aventstack.extentreports.Status;
import com.example.autoapi.config.EnvConfig;
import com.example.autoapi.notifier.NotificationService;
import com.example.autoapi.utils.ExcelSource;
import com.example.autoapi.utils.ExcelWriter;
import com.example.autoapi.utils.ParamResolver;
import com.example.autoapi.validator.ResponseValidator;
import org.testng.SkipException;

import java.util.Map;

/**
 * BaseApiTest：所有接口测试用例的基类，封装统一执行逻辑
 */
public abstract class BaseApiTest extends BaseTest {

    // Excel 路径与 sheet，由框架自动从注解中推导
    protected String filePath;
    protected String sheetName;

    /**
     * 若数据中设置了 skip=true，则跳过当前测试用例
     */
    protected void skipIfFlagged(Map<String, String> data) {
        if ("true".equalsIgnoreCase(data.getOrDefault("skip", "false"))) {
            test.skip("⚠️ 用例被标记为跳过");
            throw new SkipException("用例设置为跳过");
        }
    }

    /**
     * 测试执行主流程：请求 → 断言 → 提取 → 写入 → 通知
     */
    protected void executeAndValidate(Map<String, String> data) {
        String response = "";
        int rowIndex = Integer.parseInt(data.getOrDefault("rowIndex", "-1"));

        try {
            // 自动推导测试来源（通过反射获取注解）
            if (filePath == null || sheetName == null) {
                ExcelSource source = getClass().getMethod(Thread.currentThread().getStackTrace()[2].getMethodName(), Map.class).getAnnotation(ExcelSource.class);
                if (source != null) {
                    this.filePath = source.file();
                    this.sheetName = source.sheet();
                }
            }

            // 参数预处理
            String method = data.getOrDefault("method", "POST");
            String rawUrl = data.get("url");
            String rawBody = data.getOrDefault("body", "");
            String headersRaw = data.getOrDefault("headers", "");

            String resolvedHeaders = ParamResolver.resolveWithStore(headersRaw);
            test.log(Status.INFO, "最终请求头（已解析）: " + resolvedHeaders);

            String baseUrl = EnvConfig.get("base.url"); // 👈 读取配置
            String resolvedPath = ParamResolver.resolveWithStore(rawUrl);

            // 🧠 如果 rawUrl 是以 "/" 开头的路径，则拼接 baseUrl
            String fullUrl = resolvedPath.startsWith("http") ? resolvedPath : baseUrl + resolvedPath;

            String body = ParamResolver.resolveWithStore(rawBody);

            int expectedStatus = Integer.parseInt(data.get("expectedStatus"));

            String checkField = data.get("checkField");
            String expectedValue = data.get("expectedValue");

            String checkFields = data.get("checkFields");
            String expectedValues = data.get("expectedValues");

            String extractField = data.get("extractField");
            String storeAs = data.get("storeAs");

            test.log(Status.INFO, "请求方法: " + method);
            test.log(Status.INFO, "请求地址: " + fullUrl);
            test.log(Status.INFO, "请求体: " + body);
            test.log(Status.INFO, "请求头: " + headersRaw);

            // 执行请求
            response = ApiClient.execute(method, fullUrl, body, resolvedHeaders);
            test.log(Status.INFO, "响应体: " + response);
            System.out.println("响应体: " + response);

            // 验证响应
            ResponseValidator.validateStatusCode(200, expectedStatus);

            // 校验字段，如果字段和期望值不为空才执行
            if (checkField != null && !checkField.trim().isEmpty() && expectedValue != null && !expectedValue.trim().isEmpty()) {
                ResponseValidator.validateJsonField(response, checkField, expectedValue);
            }

            // 多字段校验，如果字段和期望值不为空才执行
            if (checkFields != null && !checkFields.trim().isEmpty() && expectedValues != null && !expectedValues.trim().isEmpty()) {
                ResponseValidator.validateMultipleJsonFields(response, checkFields, expectedValues);
            }

            // 提取字段并存储，如果字段和存储名称不为空才执行
            if (extractField != null && !extractField.trim().isEmpty() && storeAs != null && !storeAs.trim().isEmpty()) {
                ResponseValidator.extractJsonField(response, extractField, storeAs);
                test.info("📥 字段提取并缓存: ${" + storeAs + "}");
            }

            markPass(response, rowIndex);

        } catch (AssertionError ae) {
            markFail(response, rowIndex, ae.getMessage(), ae);
        } catch (Exception e) {
            markFail(response, rowIndex, e.toString(), e);
        }
    }


    /**
     * 测试通过写入日志与 Excel
     */
    protected void markPass(String response, int rowIndex) {
        test.pass("✅ 接口测试通过");
        if (rowIndex > 0) {
            ExcelWriter.writeResult(filePath, sheetName, rowIndex, response, "PASS");
        }
    }

    /**
     * 测试失败时写入错误与通知
     */
    protected void markFail(String response, int rowIndex, String msg, Throwable e) {
        String statusPrefix = "";

        // 尝试从响应中解析状态码
        if (response != null) {
            if (response.contains("\"code\":404")) statusPrefix = "FAIL-404: ";
            else if (response.contains("\"code\":401")) statusPrefix = "FAIL-401: ";
            else if (response.contains("\"code\":500")) statusPrefix = "FAIL-500: ";
            else if (response.contains("\"code\":400")) statusPrefix = "FAIL-400: ";
            else statusPrefix = "FAIL: ";
        }

        String fullMessage = statusPrefix + msg;

        test.fail("❌ 断言失败: " + fullMessage);
        NotificationService.notifyAllChannels(fullMessage);

        if (rowIndex > 0) {
            ExcelWriter.writeResult(filePath, sheetName, rowIndex, response, fullMessage);
        }

        throw new RuntimeException(fullMessage, e);
    }
}
