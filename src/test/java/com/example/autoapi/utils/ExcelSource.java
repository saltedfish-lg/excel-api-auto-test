package com.example.autoapi.utils;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExcelSource {
    String file();   // Excel文件路径
    String sheet();  // sheet名称
}
