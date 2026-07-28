package com.forge.journal.repository;

import com.forge.journal.entity.Journal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalRepository extends JpaRepository<Journal, UUID> {

    Optional<Journal> findByUserIdAndEntryDate(UUID userId, LocalDate date);

    List<Journal> findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(UUID userId, LocalDate start, LocalDate end);

    Page<Journal> findByUserIdOrderByEntryDateDesc(UUID userId, Pageable pageable);
}
