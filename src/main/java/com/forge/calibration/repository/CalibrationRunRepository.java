package com.forge.calibration.repository;

import com.forge.calibration.entity.CalibrationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CalibrationRunRepository extends JpaRepository<CalibrationRun, UUID> {

    List<CalibrationRun> findTop30ByOrderByRanAtDesc();
}
