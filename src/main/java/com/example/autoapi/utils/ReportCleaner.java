package com.example.autoapi.utils;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class ReportCleaner {

    public static void cleanOldReports(String dirPath, int keepCount) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".html"));
        if (files == null || files.length <= keepCount) return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        for (int i = keepCount; i < files.length; i++) {
            files[i].delete();
        }
    }
}
