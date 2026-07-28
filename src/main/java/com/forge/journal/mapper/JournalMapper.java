package com.forge.journal.mapper;

import com.forge.journal.dto.JournalResponse;
import com.forge.journal.entity.Journal;
import org.springframework.stereotype.Component;

@Component
public class JournalMapper {

    public JournalResponse toResponse(Journal journal) {
        return new JournalResponse(
                journal.getId(),
                journal.getEntryDate(),
                journal.getMorningGoal(),
                journal.getEveningReflection(),
                journal.getEnergy(),
                journal.getMood(),
                journal.getHoursStudied(),
                journal.getAchievements(),
                journal.getChallenges(),
                journal.getLessons()
        );
    }
}
