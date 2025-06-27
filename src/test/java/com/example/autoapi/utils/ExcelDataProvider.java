package com.example.autoapi.utils;

import com.example.autoapi.base.ExcelReader;
import org.testng.annotations.DataProvider;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 通用 Excel 数据提供器
 * 用于 TestNG 的 @DataProvider，通过 @ExcelSource 注解获取文件路径和 Sheet 名称
 *
 * 示例：
 * @Test(dataProvider = "Excel", dataProviderClass = ExcelDataProvider.class)
 * @ExcelSource(file = "src/test/resources/testcases/api_tests.xlsx", sheet = "Login")
 */
public class ExcelDataProvider {

    /**
     * 提供 Excel 数据给测试方法
     *
     * @param testMethod 当前正在执行的测试方法（反射传入）
     * @return Object[][] 包装的测试数据，每个元素是一行 Map
     */
    @DataProvider(name = "Excel")
    public static Object[][] provideExcelData(Method testMethod) {
        ExcelSource source = testMethod.getAnnotation(ExcelSource.class);
        if (source == null) {
            throw new IllegalArgumentException("❌ 缺少 @ExcelSource 注解");
        }

        List<Map<String, String>> rawData = ExcelReader.read(source.file(), source.sheet());

        // 跳过 skip=true 的行
        List<Map<String, String>> data = rawData.stream()
                .filter(row -> !"true".equalsIgnoreCase(row.getOrDefault("skip", "false")))
                .toList();

        Object[][] result = new Object[data.size()][1];
        for (int i = 0; i < data.size(); i++) {
            result[i][0] = data.get(i);
        }
        return result;
    }


}
