package com.example.autoapi.base;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.example.autoapi.auth.TokenManager;
import com.example.autoapi.notifier.EmailSender;
import com.example.autoapi.utils.ReportCleaner;
import com.example.autoapi.utils.ResponseDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.lang.reflect.Method;

public class BaseTest {

    protected static ExtentReports extent;
    protected static ExtentTest test;
    protected static String reportPath; // ✅ 改为 protected，方便子类引用

    private static ExtentSparkReporter sparkReporter;
    private static final Logger logger = LoggerFactory.getLogger(BaseTest.class);

    @BeforeSuite
    public void setupSuite() {
        // 清空变量池
        ResponseDataStore.clear();

        // 清理历史报告，仅保留最新 5 份
        ReportCleaner.cleanOldReports("reports/html", 5);

        // 初始化 Extent 报告
        reportPath = "reports/html/" + System.currentTimeMillis() + "-report.html";
        sparkReporter = new ExtentSparkReporter(reportPath);
        extent = new ExtentReports();
        extent.attachReporter(sparkReporter);
        logger.info("✅ 报告初始化完成: {}", reportPath);

        // ✅ 延迟登录，可在 BaseApiTest 控制是否登录
        if (shouldAutoLogin()) {
            TokenManager.loginAndStoreToken();
            logger.info("🔐 已自动登录并写入 token");
        }
    }

    /**
     * 是否自动登录，可在 BaseApiTest 重写控制
     */
    protected boolean shouldAutoLogin() {
        return true;
    }

    @AfterSuite
    public void tearDownSuite() {
        extent.flush();
        try {
            EmailSender.sendReport(reportPath);
            logger.info("📬 报告已发送至邮件通知通道");
        } catch (Exception e) {
            logger.error("❌ 报告发送失败: {}", e.getMessage());
        }
    }

    @BeforeMethod
    public void setupTest(Method method) {
        String testName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        test = extent.createTest(testName);
        logger.info("▶️ 开始执行用例: {}", testName);
    }

    @AfterMethod
    public void logResult(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        switch (result.getStatus()) {
            case ITestResult.SUCCESS -> {
                test.pass("✅ 用例通过");
                logger.info("✅ {} 执行成功", testName);
            }
            case ITestResult.FAILURE -> {
                test.fail("❌ 断言失败: " + result.getThrowable().getMessage());
                logger.error("❌ {} 执行失败", testName);
            }
            case ITestResult.SKIP -> {
                test.skip("⚠️ 用例被跳过");
                logger.warn("⚠️ {} 被跳过", testName);
            }
        }
    }
}
