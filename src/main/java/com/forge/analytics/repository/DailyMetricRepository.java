package com.forge.analytics.repository;

import com.forge.analytics.entity.DailyMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyMetricRepository extends JpaRepository<DailyMetric, UUID> {

    Optional<DailyMetric> findByUserIdAndMetricDate(UUID userId, LocalDate metricDate);

    List<DailyMetric> findByUserIdAndMetricDateBetweenOrderByMetricDateAsc(UUID userId, LocalDate start, LocalDate end);
}
