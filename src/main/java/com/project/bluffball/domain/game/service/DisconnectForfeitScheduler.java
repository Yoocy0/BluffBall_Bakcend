package com.project.bluffball.domain.game.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 접속 끊김 유예 만료 시 몰수패 처리를 예약·취소한다.
 */
@Component
@RequiredArgsConstructor
public class DisconnectForfeitScheduler {

    private final TaskScheduler taskScheduler;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void schedule(String matchSessionId, Long userId, Instant executeAt, Runnable task) {
        cancel(matchSessionId, userId);

        ScheduledFuture<?> future = taskScheduler.schedule(task, executeAt);
        scheduledTasks.put(taskKey(matchSessionId, userId), future);
    }

    public void cancel(String matchSessionId, Long userId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskKey(matchSessionId, userId));
        if (future != null) {
            future.cancel(false);
        }
    }

    private String taskKey(String matchSessionId, Long userId) {
        return matchSessionId + ":" + userId;
    }
}
