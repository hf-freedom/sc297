package com.dataimport.service;

import com.dataimport.entity.ImportTask;
import com.dataimport.enums.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskStorage {
    private final Map<String, ImportTask> taskMap = new ConcurrentHashMap<>();
    private final Map<String, String> runningTaskTypes = new ConcurrentHashMap<>();

    public void saveTask(ImportTask task) {
        taskMap.put(task.getId(), task);
    }

    public ImportTask getTask(String taskId) {
        return taskMap.get(taskId);
    }

    public List<ImportTask> getAllTasks() {
        List<ImportTask> tasks = new ArrayList<>(taskMap.values());
        tasks.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        return tasks;
    }

    public void removeTask(String taskId) {
        taskMap.remove(taskId);
    }

    public boolean isTaskTypeRunning(String taskType) {
        return runningTaskTypes.containsKey(taskType);
    }

    public void markTaskTypeRunning(String taskType, String taskId) {
        runningTaskTypes.put(taskType, taskId);
    }

    public void unmarkTaskTypeRunning(String taskType) {
        runningTaskTypes.remove(taskType);
    }

    public List<ImportTask> getExpiredTasks(Date expireTime) {
        List<ImportTask> expiredTasks = new ArrayList<>();
        for (ImportTask task : taskMap.values()) {
            if (task.getEndTime() != null && task.getEndTime().before(expireTime)) {
                expiredTasks.add(task);
            }
        }
        return expiredTasks;
    }

    public List<ImportTask> getTasksByStatus(TaskStatus status) {
        List<ImportTask> result = new ArrayList<>();
        for (ImportTask task : taskMap.values()) {
            if (task.getStatus() == status) {
                result.add(task);
            }
        }
        return result;
    }
}
