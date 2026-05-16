package com.dataimport.enums;

public enum DeduplicateStrategy {
    SKIP("跳过"),
    OVERWRITE("覆盖"),
    MERGE("合并");

    private String desc;

    DeduplicateStrategy(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
