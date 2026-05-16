package com.dataimport.entity;

import com.dataimport.enums.DeduplicateStrategy;
import com.dataimport.enums.TaskPriority;
import com.dataimport.enums.TaskStatus;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ImportTask {
    private String id;
    private String taskName;
    private String taskType;
    private String fileName;
    private Long fileSize;
    private TaskStatus status;
    private String statusDesc;
    private DeduplicateStrategy deduplicateStrategy;
    private TaskPriority priority;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer duplicateCount;
    private List<Map<String, Object>> validData;
    private List<ErrorRecord> errorList;
    private List<Map<String, Object>> duplicateData;
    private Map<String, Object> cacheData;
    private Date createTime;
    private Date startTime;
    private Date endTime;
    private String errorMessage;
    private Integer retryCount;
    private String progress;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getStatusDesc() { return statusDesc; }
    public void setStatusDesc(String statusDesc) { this.statusDesc = statusDesc; }
    public DeduplicateStrategy getDeduplicateStrategy() { return deduplicateStrategy; }
    public void setDeduplicateStrategy(DeduplicateStrategy deduplicateStrategy) { this.deduplicateStrategy = deduplicateStrategy; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }
    public Integer getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(Integer duplicateCount) { this.duplicateCount = duplicateCount; }
    public List<Map<String, Object>> getValidData() { return validData; }
    public void setValidData(List<Map<String, Object>> validData) { this.validData = validData; }
    public List<ErrorRecord> getErrorList() { return errorList; }
    public void setErrorList(List<ErrorRecord> errorList) { this.errorList = errorList; }
    public List<Map<String, Object>> getDuplicateData() { return duplicateData; }
    public void setDuplicateData(List<Map<String, Object>> duplicateData) { this.duplicateData = duplicateData; }
    public Map<String, Object> getCacheData() { return cacheData; }
    public void setCacheData(Map<String, Object> cacheData) { this.cacheData = cacheData; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public String getProgress() { return progress; }
    public void setProgress(String progress) { this.progress = progress; }
}
