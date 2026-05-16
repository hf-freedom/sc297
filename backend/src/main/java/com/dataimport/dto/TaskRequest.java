package com.dataimport.dto;

import com.dataimport.enums.DeduplicateStrategy;
import com.dataimport.enums.TaskPriority;

public class TaskRequest {
    private String taskName;
    private String taskType;
    private DeduplicateStrategy deduplicateStrategy;
    private TaskPriority priority;

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public DeduplicateStrategy getDeduplicateStrategy() { return deduplicateStrategy; }
    public void setDeduplicateStrategy(DeduplicateStrategy deduplicateStrategy) { this.deduplicateStrategy = deduplicateStrategy; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
}
