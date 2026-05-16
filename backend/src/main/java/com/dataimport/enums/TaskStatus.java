package com.dataimport.enums;

public enum TaskStatus {
    PENDING("待处理"),
    QUEUED("排队中"),
    VALIDATING("校验中"),
    PARSING("解析中"),
    DEDUPLICATING("去重中"),
    CACHING("入缓存中"),
    STATISTICS("统计中"),
    SUCCESS("成功"),
    FAILED("失败"),
    CANCELLED("已取消");

    private String desc;

    TaskStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
