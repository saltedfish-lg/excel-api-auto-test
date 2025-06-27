package com.example.autoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ExcelWriter 工具类：用于将测试结果写入 Excel（线程安全）
 */
public class ExcelWriter {

    // 确保并发写入线程安全
    private static final ReentrantLock lock = new ReentrantLock();

    /**
     * 写入测试结果到 Excel 文件的指定 Sheet 和行
     *
     * @param filePath  Excel 文件路径
     * @param sheetName Sheet 名称
     * @param rowIndex  Excel 行号（注意：从 1 开始，自动转换为 POI 的 0-based）
     * @param response  接口响应数据
     * @param status    测试状态（PASS / FAIL）
     */
    public static void writeResult(String filePath, String sheetName, int rowIndex, String response, String status) {
        lock.lock(); // 锁定资源，确保线程安全
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) throw new RuntimeException("❌ Sheet 不存在: " + sheetName);

            // POI 是从 0 开始的行号，rowIndex 是 Excel 中的（从 1 开始）
            Row row = sheet.getRow(rowIndex - 1);
            if (row == null) row = sheet.createRow(rowIndex - 1);

            int responseCol = findOrCreateColumnIndex(sheet, "response");
            int statusCol = findOrCreateColumnIndex(sheet, "status");

            Cell responseCell = row.getCell(responseCol);
            if (responseCell == null) responseCell = row.createCell(responseCol);
            responseCell.setCellValue(response);

            Cell statusCell = row.getCell(statusCol);
            if (statusCell == null) statusCell = row.createCell(statusCol);
            statusCell.setCellValue(status);

            // 关闭输入流，防止 Windows 下无法写入
            fis.close();

            // 写回文件
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
                fos.flush();
            }

        } catch (Exception e) {
            throw new RuntimeException("❌ 写入 Excel 失败: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 查找列名对应的列号，如果不存在则创建新列
     *
     * @param sheet 当前 Sheet
     * @param field 字段名（列名）
     * @return 列的索引（0-based）
     */
    private static int findOrCreateColumnIndex(Sheet sheet, String field) {
        Row header = sheet.getRow(0);
        if (header == null) header = sheet.createRow(0);

        int colCount = header.getLastCellNum();
        if (colCount < 0) colCount = 0; // 避免 getLastCellNum() 返回 -1

        for (int i = 0; i < colCount; i++) {
            Cell cell = header.getCell(i);
            if (cell != null && field.equalsIgnoreCase(cell.getStringCellValue().trim())) {
                return i;
            }
        }

        // 未找到，则创建新列
        Cell newHeader = header.createCell(colCount);
        newHeader.setCellValue(field);
        return colCount;
    }
}
