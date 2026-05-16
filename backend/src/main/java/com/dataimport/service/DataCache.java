package com.dataimport.service;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataCache {
    private final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();
    private final Set<String> processedRecordIds = ConcurrentHashMap.newKeySet();

    public void save(String key, Map<String, Object> data) {
        cache.put(key, data);
    }

    public Map<String, Object> get(String key) {
        return cache.get(key);
    }

    public void remove(String key) {
        cache.remove(key);
    }

    public boolean containsKey(String key) {
        return cache.containsKey(key);
    }

    public List<Map<String, Object>> getAll() {
        return new ArrayList<>(cache.values());
    }

    public void markRecordProcessed(String recordId) {
        processedRecordIds.add(recordId);
    }

    public boolean isRecordProcessed(String recordId) {
        return processedRecordIds.contains(recordId);
    }

    public void clearProcessedRecords() {
        processedRecordIds.clear();
    }

    public int size() {
        return cache.size();
    }
}
