package com.forge.journal.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.dto.PagedResponse;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.util.SecurityUtils;
import com.forge.journal.dto.JournalRequest;
import com.forge.journal.dto.JournalResponse;
import com.forge.journal.entity.Journal;
import com.forge.journal.mapper.JournalMapper;
import com.forge.journal.repository.JournalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalRepository journalRepository;
    private final UserRepository userRepository;
    private final JournalMapper journalMapper;

    public PagedResponse<JournalResponse> getJournals(int page, int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("entryDate").descending());
        Page<Journal> journalPage = journalRepository.findByUserIdOrderByEntryDateDesc(userId, pageRequest);

        List<JournalResponse> content = journalPage.getContent().stream()
                .map(journalMapper::toResponse)
                .toList();

        return new PagedResponse<>(content, page, size, journalPage.getTotalElements(), journalPage.getTotalPages(), journalPage.isLast());
    }

    public JournalResponse getTodayJournal() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return journalRepository.findByUserIdAndEntryDate(userId, LocalDate.now())
                .map(journalMapper::toResponse)
                .orElse(null);
    }

    public List<JournalResponse> getRecentJournals() {
        UUID userId = SecurityUtils.getCurrentUserId();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(7);
        return journalRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(userId, start, end).stream()
                .map(journalMapper::toResponse)
                .toList();
    }

    public JournalResponse createOrUpdateJournal(JournalRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        LocalDate entryDate = request.getEntryDate() != null ? request.getEntryDate() : LocalDate.now();

        Journal journal = journalRepository.findByUserIdAndEntryDate(userId, entryDate)
                .orElse(new Journal());

        journal.setUser(user);
        journal.setEntryDate(entryDate);
        journal.setMorningGoal(request.getMorningGoal());
        journal.setEveningReflection(request.getEveningReflection());
        journal.setEnergy(request.getEnergy());
        journal.setMood(request.getMood());
        journal.setHoursStudied(request.getHoursStudied() != null ? request.getHoursStudied() : 0.0);
        journal.setAchievements(request.getAchievements());
        journal.setChallenges(request.getChallenges());
        journal.setLessons(request.getLessons());

        journal = journalRepository.save(journal);
        log.info("Journal saved for date: {} by user: {}", entryDate, userId);
        return journalMapper.toResponse(journal);
    }
}
