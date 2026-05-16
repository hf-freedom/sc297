package com.dataimport.controller;

import com.dataimport.common.Result;
import com.dataimport.entity.ImportTask;
import com.dataimport.enums.DeduplicateStrategy;
import com.dataimport.enums.TaskPriority;
import com.dataimport.service.DataCache;
import com.dataimport.service.TaskExecutorService;
import com.dataimport.service.TaskStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskExecutorService taskExecutorService;

    @Autowired
    private TaskStorage taskStorage;

    @Autowired
    private DataCache dataCache;

    @PostMapping("/upload")
    public Result<ImportTask> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("taskName") String taskName,
            @RequestParam("taskType") String taskType,
            @RequestParam(value = "deduplicateStrategy", required = false) DeduplicateStrategy strategy,
            @RequestParam(value = "priority", required = false) TaskPriority priority) {
        try {
            ImportTask task = taskExecutorService.createTask(file, taskName, taskType, strategy, priority);
            return Result.success(task);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping
    public Result<List<ImportTask>> getAllTasks() {
        return Result.success(taskStorage.getAllTasks());
    }

    @GetMapping("/{taskId}")
    public Result<ImportTask> getTaskDetail(@PathVariable String taskId) {
        ImportTask task = taskStorage.getTask(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }
        return Result.success(task);
    }

    @PostMapping("/{taskId}/retry")
    public Result<ImportTask> retryTask(@PathVariable String taskId) {
        try {
            ImportTask task = taskExecutorService.retryTask(taskId);
            return Result.success(task);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{taskId}")
    public Result<?> deleteTask(@PathVariable String taskId) {
        ImportTask task = taskStorage.getTask(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }
        taskStorage.removeTask(taskId);
        return Result.success();
    }

    @GetMapping("/cache/data")
    public Result<List<Map<String, Object>>> getCacheData() {
        return Result.success(dataCache.getAll());
    }

    @GetMapping("/cache/size")
    public Result<Integer> getCacheSize() {
        return Result.success(dataCache.size());
    }

    @GetMapping("/check-type/{taskType}")
    public Result<Map<String, Object>> checkTaskTypeRunning(@PathVariable String taskType) {
        boolean isRunning = taskStorage.isTaskTypeRunning(taskType);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("isRunning", isRunning);
        if (isRunning) {
            result.put("message", "该类型任务正在执行中，请等待完成后再提交");
        }
        return Result.success(result);
    }
}
