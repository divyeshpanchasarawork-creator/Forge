package com.forge.journal.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.dto.PagedResponse;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.util.SecurityUtils;
import com.forge.common.util.TimezoneUtil;
import com.forge.journal.dto.JournalRequest;
import com.forge.journal.dto.JournalResponse;
import com.forge.journal.entity.Journal;
import com.forge.journal.mapper.JournalMapper;
import com.forge.journal.repository.JournalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final PlatformTransactionManager transactionManager;

    @Transactional(readOnly = true)
    public PagedResponse<JournalResponse> getJournals(int page, int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("entryDate").descending());
        Page<Journal> journalPage = journalRepository.findByUserIdOrderByEntryDateDesc(userId, pageRequest);

        List<JournalResponse> content = journalPage.getContent().stream()
                .map(journalMapper::toResponse)
                .toList();

        return new PagedResponse<>(content, page, size, journalPage.getTotalElements(), journalPage.getTotalPages(), journalPage.isLast());
    }

    public JournalResponse createOrUpdateJournal(JournalRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        LocalDate entryDate = request.getEntryDate() != null
                ? request.getEntryDate()
                : LocalDate.now(TimezoneUtil.resolve(user));

        Journal journal = findOrCreate(userId, entryDate);
        apply(journal, user, entryDate, request);

        try {
            journal = saveJournal(journal);
        } catch (DataIntegrityViolationException e) {
            Journal fresh = findOrCreate(userId, entryDate);
            apply(fresh, user, entryDate, request);
            journal = saveJournal(fresh);
        }
        log.info("Journal saved for date: {} by user: {}", entryDate, userId);
        return journalMapper.toResponse(journal);
    }

    /**
     * Persists in its own transaction so a {@link DataIntegrityViolationException} (concurrent
     * same-day upsert) rolls back cleanly and the retry runs against a fresh persistence context.
     * A retry inside the surrounding transaction would be doomed by the rollback-only marker.
     */
    private Journal saveJournal(Journal journal) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx.execute(status -> journalRepository.save(journal));
    }

    private void apply(Journal journal, User user, LocalDate entryDate, JournalRequest request) {
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
    }

    private Journal findOrCreate(UUID userId, LocalDate entryDate) {
        return journalRepository.findByUserIdAndEntryDate(userId, entryDate)
                .orElseGet(Journal::new);
    }
}
