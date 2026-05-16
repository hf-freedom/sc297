package com.dataimport.service;

import com.dataimport.entity.ErrorRecord;
import com.dataimport.entity.ImportTask;
import com.dataimport.enums.DeduplicateStrategy;
import com.dataimport.enums.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.*;

@Service
public class TaskExecutorService {

    @Autowired
    private TaskStorage taskStorage;

    @Autowired
    private FileParserService fileParserService;

    @Autowired
    private DataCache dataCache;

    @Value("${import.queue.max-concurrent:2}")
    private int maxConcurrentTasks;

    @Value("${import.queue.large-file-threshold:10485760}")
    private long largeFileThreshold;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final PriorityBlockingQueue<ImportTask> taskQueue = new PriorityBlockingQueue<>(
            100,
            Comparator.comparingInt((ImportTask t) -> t.getPriority().getLevel())
    );
    private final Set<String> runningTaskIds = ConcurrentHashMap.newKeySet();
    private volatile boolean queueProcessorRunning = false;

    public ImportTask createTask(MultipartFile file, String taskName, String taskType,
                                  DeduplicateStrategy strategy, com.dataimport.enums.TaskPriority priority) throws Exception {

        if (taskStorage.isTaskTypeRunning(taskType)) {
            throw new Exception("该类型任务正在执行中，不能重复提交");
        }

        ImportTask task = new ImportTask();
        task.setId(UUID.randomUUID().toString());
        task.setTaskName(taskName);
        task.setTaskType(taskType);
        task.setFileName(file.getOriginalFilename());
        task.setFileSize(file.getSize());
        task.setStatus(TaskStatus.PENDING);
        task.setDeduplicateStrategy(strategy != null ? strategy : DeduplicateStrategy.SKIP);
        task.setPriority(priority != null ? priority : com.dataimport.enums.TaskPriority.MEDIUM);
        task.setCreateTime(new Date());
        task.setRetryCount(0);
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setDuplicateCount(0);
        task.setErrorList(new ArrayList<>());
        task.setDuplicateData(new ArrayList<>());

        taskStorage.saveTask(task);

        if (file.getSize() > largeFileThreshold) {
            task.setStatus(TaskStatus.QUEUED);
            task.setStatusDesc("已进入排队池等待执行");
            taskQueue.offer(task);
            startQueueProcessor();
        } else {
            executorService.submit(() -> executeTask(task, file));
        }

        return task;
    }

    private void startQueueProcessor() {
        if (!queueProcessorRunning) {
            synchronized (this) {
                if (!queueProcessorRunning) {
                    queueProcessorRunning = true;
                    new Thread(this::processQueue).start();
                }
            }
        }
    }

    private void processQueue() {
        while (true) {
            try {
                if (runningTaskIds.size() < maxConcurrentTasks) {
                    ImportTask task = taskQueue.poll(5, TimeUnit.SECONDS);
                    if (task != null) {
                        runningTaskIds.add(task.getId());
                        executorService.submit(() -> {
                            try {
                                executeTask(task, null);
                            } finally {
                                runningTaskIds.remove(task.getId());
                            }
                        });
                    }
                } else {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void executeTask(ImportTask task, MultipartFile file) {
        try {
            taskStorage.markTaskTypeRunning(task.getTaskType(), task.getId());
            task.setStartTime(new Date());

            List<Map<String, Object>> rawData = null;
            if (file != null) {
                task.setStatus(TaskStatus.PARSING);
                task.setStatusDesc("正在解析文件");
                task.setProgress("20%");
                taskStorage.saveTask(task);
                rawData = fileParserService.parseFile(file);
            } else if (task.getValidData() != null) {
                rawData = task.getValidData();
            }

            if (rawData == null || rawData.isEmpty()) {
                throw new Exception("没有可解析的数据");
            }

            task.setTotalCount(rawData.size());

            task.setStatus(TaskStatus.VALIDATING);
            task.setStatusDesc("正在校验数据");
            task.setProgress("40%");
            taskStorage.saveTask(task);

            List<Map<String, Object>> validData = new ArrayList<>();
            List<ErrorRecord> errorList = new ArrayList<>();

            for (int i = 0; i < rawData.size(); i++) {
                Map<String, Object> row = rawData.get(i);
                String validationError = validateRow(row);
                if (validationError == null) {
                    validData.add(row);
                } else {
                    ErrorRecord error = new ErrorRecord();
                    error.setRowIndex(i + 2);
                    error.setData(row);
                    error.setErrorMessage(validationError);
                    errorList.add(error);
                }
            }

            task.setErrorList(errorList);
            task.setFailedCount(errorList.size());
            task.setValidData(validData);

            task.setStatus(TaskStatus.DEDUPLICATING);
            task.setStatusDesc("正在去重处理");
            task.setProgress("60%");
            taskStorage.saveTask(task);

            List<Map<String, Object>> uniqueData = new ArrayList<>();
            List<Map<String, Object>> duplicateData = new ArrayList<>();
            Set<String> keys = new HashSet<>();

            for (Map<String, Object> row : validData) {
                String key = generateUniqueKey(row);
                if (key == null) {
                    uniqueData.add(row);
                    continue;
                }

                if (keys.contains(key) || dataCache.containsKey(key)) {
                    duplicateData.add(row);
                    handleDuplicate(row, key, task.getDeduplicateStrategy());
                } else {
                    keys.add(key);
                    uniqueData.add(row);
                }
            }

            task.setDuplicateData(duplicateData);
            task.setDuplicateCount(duplicateData.size());

            task.setStatus(TaskStatus.CACHING);
            task.setStatusDesc("正在写入缓存");
            task.setProgress("80%");
            taskStorage.saveTask(task);

            int successCount = 0;
            for (Map<String, Object> row : uniqueData) {
                String key = generateUniqueKey(row);
                if (key != null && !dataCache.isRecordProcessed(key)) {
                    dataCache.save(key, row);
                    dataCache.markRecordProcessed(key);
                    successCount++;
                }
            }
            task.setSuccessCount(task.getSuccessCount() + successCount);

            task.setStatus(TaskStatus.STATISTICS);
            task.setStatusDesc("正在统计结果");
            task.setProgress("95%");
            taskStorage.saveTask(task);

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("total", task.getTotalCount());
            statistics.put("success", task.getSuccessCount());
            statistics.put("failed", task.getFailedCount());
            statistics.put("duplicate", task.getDuplicateCount());
            task.setCacheData(statistics);

            task.setStatus(TaskStatus.SUCCESS);
            task.setStatusDesc("任务完成");
            task.setProgress("100%");
            task.setEndTime(new Date());

        } catch (Exception e) {
            task.setStatus(TaskStatus.FAILED);
            task.setStatusDesc("任务失败");
            task.setErrorMessage(e.getMessage());
            task.setEndTime(new Date());
        } finally {
            taskStorage.unmarkTaskTypeRunning(task.getTaskType());
            taskStorage.saveTask(task);
        }
    }

    private String validateRow(Map<String, Object> row) {
        if (row.isEmpty()) {
            return "空行数据";
        }

        for (Object value : row.values()) {
            if (value == null || value.toString().trim().isEmpty()) {
                return "存在空字段";
            }
        }

        return null;
    }

    private String generateUniqueKey(Map<String, Object> row) {
        if (row.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (Object value : row.values()) {
            if (value != null) {
                sb.append(value.toString()).append("|");
            }
        }
        return sb.toString();
    }

    private void handleDuplicate(Map<String, Object> row, String key, DeduplicateStrategy strategy) {
        switch (strategy) {
            case OVERWRITE:
                if (!dataCache.isRecordProcessed(key)) {
                    dataCache.save(key, row);
                }
                break;
            case MERGE:
                Map<String, Object> existing = dataCache.get(key);
                if (existing != null) {
                    Map<String, Object> merged = new HashMap<>(existing);
                    merged.putAll(row);
                    dataCache.save(key, merged);
                }
                break;
            case SKIP:
            default:
                break;
        }
    }

    public ImportTask retryTask(String taskId) throws Exception {
        ImportTask task = taskStorage.getTask(taskId);
        if (task == null) {
            throw new Exception("任务不存在");
        }

        if (task.getStatus() != TaskStatus.FAILED) {
            throw new Exception("只有失败的任务才能重试");
        }

        if (taskStorage.isTaskTypeRunning(task.getTaskType())) {
            throw new Exception("该类型任务正在执行中，不能重复提交");
        }

        task.setStatus(TaskStatus.PENDING);
        task.setStatusDesc("准备重试");
        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(null);
        taskStorage.saveTask(task);

        executorService.submit(() -> executeTask(task, null));

        return task;
    }
}
