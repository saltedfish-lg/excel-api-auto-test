package com.example.autoapi.utils;

import java.util.concurrent.ConcurrentHashMap;

public class ResponseDataStore {

    // 线程安全的全局变量池
    private static final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    public static void put(String key, String value) {
        store.put(key, value);
    }

    public static String get(String key) {
        return store.get(key);
    }

    public static void remove(String key) {
        store.remove(key);
    }

    public static boolean contains(String key) {
        return store.containsKey(key);
    }

    public static void clear() {
        store.clear();
    }

    public static ConcurrentHashMap<String, String> getAll() {
        return store;
    }
}
