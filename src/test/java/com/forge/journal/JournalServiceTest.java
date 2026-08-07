package com.forge.journal;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.util.SecurityUtils;
import com.forge.journal.dto.JournalRequest;
import com.forge.journal.entity.Journal;
import com.forge.journal.mapper.JournalMapper;
import com.forge.journal.repository.JournalRepository;
import com.forge.journal.service.JournalService;
import com.forge.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    @Mock private JournalRepository journalRepository;
    @Mock private UserRepository userRepository;

    private JournalService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new JournalService(journalRepository, userRepository, new JournalMapper());
        userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "testuser", "password", "USER");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrUpdateJournalRetriesWhenConcurrentInsertWinsTheRace() {
        User user = new User();
        user.setId(userId);
        user.setTimezone("UTC");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Journal existing = new Journal();
        existing.setId(UUID.randomUUID());
        existing.setUser(user);
        existing.setEntryDate(LocalDate.of(2026, 8, 6));

        when(journalRepository.findByUserIdAndEntryDate(eq(userId), eq(LocalDate.of(2026, 8, 6))))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(journalRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate (user_id, entry_date)"))
                .thenAnswer(inv -> inv.getArgument(0));

        JournalRequest request = new JournalRequest();
        request.setEntryDate(LocalDate.of(2026, 8, 6));
        request.setMorningGoal("Finish DP practice");

        var response = service.createOrUpdateJournal(request);

        verify(journalRepository, times(2)).save(any());
        assertEquals("Finish DP practice", response.getMorningGoal());
    }

    @Test
    void getTodayJournalQueriesWithUserTimezone() {
        User user = new User();
        user.setId(userId);
        user.setTimezone("America/New_York");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(journalRepository.findByUserIdAndEntryDate(eq(userId), any()))
                .thenReturn(Optional.empty());

        service.getTodayJournal();

        verify(journalRepository).findByUserIdAndEntryDate(userId, LocalDate.now(ZoneId.of("America/New_York")));
    }
}
