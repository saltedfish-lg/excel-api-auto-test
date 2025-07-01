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
        lock.lock(); // 确保线程安全
        Workbook workbook = null;

        try (FileInputStream fis = new FileInputStream(filePath)) {
            workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) throw new RuntimeException("❌ Sheet 不存在: " + sheetName);

            Row row = sheet.getRow(rowIndex - 1); // Excel 从 1 开始，POI 从 0 开始
            if (row == null) row = sheet.createRow(rowIndex - 1);

            // 获取“response”和“status”列索引
            int responseCol = findOrCreateColumnIndex(sheet, "response");
            int statusCol = findOrCreateColumnIndex(sheet, "status");

            // 写入响应内容
            writeCell(row, responseCol, response);
            writeCell(row, statusCol, status);

        } catch (Exception e) {
            throw new RuntimeException("❌ 写入 Excel 失败: " + e.getMessage(), e);
        } finally {
            // 始终尝试写入，前提是 workbook 不为 null（且不抛出写入异常就关闭文件）
            if (workbook != null) {
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    workbook.write(fos);
                    fos.flush();
                } catch (IOException ioException) {
                    throw new RuntimeException("❌ Excel 写入时出错: " + ioException.getMessage(), ioException);
                }
            }
            lock.unlock(); // 释放锁
        }
    }

    private static void writeCell(Row row, int colIndex, String value) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) cell = row.createCell(colIndex);
        cell.setCellValue(value != null ? value : "");
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
