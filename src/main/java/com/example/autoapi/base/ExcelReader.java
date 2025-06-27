package com.example.autoapi.base;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.*;

/**
 * ExcelReader 工具类：用于读取 Excel 中的所有 sheet 页数据或指定 sheet 页数据
 */
public class ExcelReader {

    /**
     * 读取 Excel 中所有 Sheet 的数据，按 Sheet 名分组返回
     *
     * @param filePath Excel 文件路径
     * @return 每个 Sheet 名 -> 对应的 List<Map<String, String>> 行数据
     */
    public static Map<String, List<Map<String, String>>> readAllSheets(String filePath) {
        Map<String, List<Map<String, String>>> allSheetData = new HashMap<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(filePath))) {
            for (Sheet sheet : workbook) {
                String sheetName = sheet.getSheetName();
                List<Map<String, String>> sheetData = new ArrayList<>();

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;

                int colCount = headerRow.getLastCellNum();

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    Map<String, String> rowData = new LinkedHashMap<>();
                    boolean isEmptyRow = true;

                    for (int j = 0; j < colCount; j++) {
                        Cell headerCell = headerRow.getCell(j);
                        Cell cell = row.getCell(j);

                        String header = formatter.formatCellValue(headerCell).trim();
                        String value = formatter.formatCellValue(cell).trim();

                        if (!header.isEmpty()) {
                            rowData.put(header, value);
                        }

                        if (!value.isEmpty()) {
                            isEmptyRow = false;
                        }
                    }

                    if (!isEmptyRow && !rowData.isEmpty()) {
                        rowData.put("rowIndex", String.valueOf(i + 1));
                        sheetData.add(rowData);
                    }
                }

                allSheetData.put(sheetName, sheetData);
            }

        } catch (Exception e) {
            throw new RuntimeException("❌ Excel读取失败: " + e.getMessage(), e);
        }

        return allSheetData;
    }


    /**
     * 读取指定 Sheet 页的数据
     *
     * @param filePath  Excel 文件路径
     * @param sheetName Sheet 名称
     * @return List<Map<String, String>> 表示每一行数据
     */
    public static List<Map<String, String>> read(String filePath, String sheetName) {
        return readAllSheets(filePath).getOrDefault(sheetName, new ArrayList<>());
    }
}
