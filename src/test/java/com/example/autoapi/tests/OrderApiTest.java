package com.example.autoapi.tests;

import com.example.autoapi.base.BaseApiTest;
import com.example.autoapi.base.RetryAnalyzer;
import com.example.autoapi.utils.ExcelDataProvider;
import com.example.autoapi.utils.ExcelSource;
import org.testng.annotations.Test;

import java.util.Map;

public class OrderApiTest extends BaseApiTest {

    @Test(dataProvider = "Excel", dataProviderClass = ExcelDataProvider.class, retryAnalyzer = RetryAnalyzer.class)
    @ExcelSource(file = "src/test/resources/testcases/api_tests.xlsx", sheet = "Order")
    public void GetformInfoList(Map<String, String> data) {
        executeAndValidate(data);
    }
}
