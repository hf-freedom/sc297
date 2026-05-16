package com.dataimport.enums;

public enum TaskPriority {
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private int level;

    TaskPriority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
