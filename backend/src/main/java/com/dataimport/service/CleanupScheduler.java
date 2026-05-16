package com.dataimport.service;

import com.dataimport.entity.ImportTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class CleanupScheduler {

    @Autowired
    private TaskStorage taskStorage;

    @Value("${task.cleanup.expire-hours:24}")
    private int expireHours;

    @Scheduled(cron = "${task.cleanup.cron:0 0 2 * * ?}")
    public void cleanupExpiredTasks() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -expireHours);
        Date expireTime = calendar.getTime();

        List<ImportTask> expiredTasks = taskStorage.getExpiredTasks(expireTime);

        for (ImportTask task : expiredTasks) {
            taskStorage.removeTask(task.getId());
        }
    }
}
