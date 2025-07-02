package com.example.autoapi.listeners;

import com.example.autoapi.notifier.WeComNotifier;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.ArrayList;
import java.util.List;

public class TestResultListener implements ITestListener {

    // 用于存储失败的用例和失败的原因
    private List<String> failedTests = new ArrayList<>();
    private int passedTests = 0;
    private int failedTestsCount = 0;

    @Override
    public void onTestStart(ITestResult result) {
        // 当一个测试用例开始时触发（这里不需要处理）
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        // 当测试用例成功时触发
        passedTests++;
    }

    @Override
    public void onTestFailure(ITestResult result) {
        // 当测试用例失败时触发
        failedTestsCount++;
        String testName = result.getName();
        String failureReason = result.getThrowable().getMessage();
        failedTests.add("Test case: " + testName + "\nFailure Reason: " + failureReason);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        // 当测试用例跳过时触发（这里不需要处理）
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        // 当测试用例在失败但仍在成功百分比内时触发（这里不需要处理）
    }

    @Override
    public void onStart(ITestContext context) {
        // 在测试开始时触发（这里不需要处理）
    }

    @Override
    public void onFinish(ITestContext context) {
        // 在测试完成时触发，发送最终测试报告
        sendTestReport();
    }

    /**
     * 发送测试报告的企业微信消息
     */
    private void sendTestReport() {
        StringBuilder report = new StringBuilder();
        report.append("🎯 测试用例执行完成\n\n");
        report.append("🔢 总共执行用例: ").append(passedTests + failedTestsCount).append("\n");
        report.append("✅ 成功用例: ").append(passedTests).append("\n");
        report.append("❌ 失败用例: ").append(failedTestsCount).append("\n\n");

        if (!failedTests.isEmpty()) {
            report.append("❌ 失败的用例及失败原因: \n");
            for (String failedTest : failedTests) {
                report.append(failedTest).append("\n\n");
            }
        } else {
            report.append("🎉 所有用例执行成功！\n");
        }

        // 发送报告到企业微信
        WeComNotifier.sendText(report.toString());
    }
}
