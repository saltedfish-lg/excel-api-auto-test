package com.example.autoapi.base;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.SkipException;

public class RetryAnalyzer implements IRetryAnalyzer {

    private int retryCount = 0;
    private final int maxRetryCount = 1;

    @Override
    public boolean retry(ITestResult result) {
        // 跳过的用例不重试
        if (result.getThrowable() instanceof SkipException) {
            return false;
        }

        // 仅当断言失败时重试
        if (result.getThrowable() instanceof AssertionError && retryCount < maxRetryCount) {
            retryCount++;
            return true;
        }

        return false;
    }
}



