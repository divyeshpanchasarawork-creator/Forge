package com.forge.scheduler;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SchedulerStatus {

    public record Status(
            Instant lastRunAt,
            int candidates,
            int processed,
            int skippedOutsideWindow,
            int skippedQuota,
            String error
    ) {
        static Status empty() {
            return new Status(null, 0, 0, 0, 0, null);
        }
    }

    private final AtomicReference<Status> last = new AtomicReference<>(Status.empty());

    public void record(Instant lastRunAt, int candidates, int processed, int skippedOutsideWindow, int skippedQuota, String error) {
        last.set(new Status(lastRunAt, candidates, processed, skippedOutsideWindow, skippedQuota, error));
    }

    public Status status() {
        return last.get();
    }
}
