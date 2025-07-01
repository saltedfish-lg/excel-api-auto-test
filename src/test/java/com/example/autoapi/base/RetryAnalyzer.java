package com.example.autoapi.base;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.SkipException;

public class RetryAnalyzer implements IRetryAnalyzer {

    private int retryCount = 0;
    private final int maxRetryCount = 1;

    @Override
    public boolean retry(ITestResult result) {
        Throwable throwable = result.getThrowable();

        // 1. 跳过的用例不重试
        if (throwable instanceof SkipException) {
            return false;
        }

        // 2. 只重试断言失败
        if (throwable instanceof AssertionError && retryCount < maxRetryCount) {
            String message = throwable.getMessage();

            // 🚫 检查错误类型是否为不可重试类型
            if (message != null) {
                if (message.contains("404") || message.contains("FAIL-404")) return false;
                if (message.contains("401") || message.contains("FAIL-401")) return false;
                if (message.contains("接口不存在") || message.contains("路径错误")) return false;
            }

            retryCount++;
            return true;
        }

        return false;
    }
}




