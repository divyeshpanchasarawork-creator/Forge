package com.forge.intelligence.service;

import com.forge.common.util.TimezoneUtil;
import com.forge.topic.entity.Topic;
import org.springframework.stereotype.Service;

@Service
public class MasteryService {

    public int qualityFrom(String outcome, int hintsUsed, Integer timeTakenSeconds) {
        int hints = Math.max(0, hintsUsed);
        int quality = switch (outcome) {
            case "SOLVED" -> hints == 0 ? 5 : Math.max(1, 5 - hints);
            case "PARTIAL" -> hints == 0 ? 3 : 2;
            case "FAILED" -> 1;
            case "SKIPPED" -> 0;
            default -> 2;
        };
        if (timeTakenSeconds != null && timeTakenSeconds > 60 * 45) {
            quality = Math.max(0, quality - 1);
        }
        return Math.max(0, Math.min(5, quality));
    }

    public void apply(Topic topic, String outcome, int hintsUsed) {
        int mastery = topic.getMastery() != null ? topic.getMastery() : 0;
        int confidence = topic.getConfidence() != null ? topic.getConfidence() : 0;

        if ("SOLVED".equals(outcome)) {
            int gain = Math.max(3, (int) Math.round((100 - mastery) * 0.10));
            mastery = Math.min(100, mastery + gain);
            confidence = Math.min(10, confidence + 1);
        } else if ("PARTIAL".equals(outcome)) {
            mastery = Math.min(100, mastery + 4);
            if (hintsUsed > 2) {
                confidence = Math.max(0, confidence - 1);
            }
        } else if ("FAILED".equals(outcome)) {
            mastery = Math.max(0, mastery - 8);
            confidence = Math.max(0, confidence - 1);
        }

        topic.setMastery(mastery);
        topic.setConfidence(confidence);
        topic.setAttemptsTotal((topic.getAttemptsTotal() != null ? topic.getAttemptsTotal() : 0) + 1);
        if ("SOLVED".equals(outcome) || "PARTIAL".equals(outcome)) {
            topic.setAttemptsSolved((topic.getAttemptsSolved() != null ? topic.getAttemptsSolved() : 0) + 1);
        }
        topic.setLastAttemptAt(TimezoneUtil.now(topic.getUser()));

        if (mastery >= 80) {
            topic.setStatus("MASTERED");
        } else if (mastery > 0 || topic.getAttemptsTotal() > 0) {
            topic.setStatus("IN_PROGRESS");
        }
    }
}
