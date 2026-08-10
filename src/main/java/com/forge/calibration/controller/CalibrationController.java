package com.forge.calibration.controller;

import com.forge.calibration.dto.CalibrationResult;
import com.forge.calibration.dto.EngineReport;
import com.forge.calibration.service.CalibrationJob;
import com.forge.calibration.service.EngineReportService;
import com.forge.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class CalibrationController {

    private final EngineReportService engineReportService;
    private final CalibrationJob calibrationJob;

    @GetMapping("/engine-report")
    public ResponseEntity<ApiResponse<EngineReport>> engineReport() {
        return ResponseEntity.ok(ApiResponse.success(engineReportService.getReport()));
    }

    @PostMapping("/calibration/run")
    public ResponseEntity<ApiResponse<CalibrationResult>> runCalibration() {
        return ResponseEntity.ok(ApiResponse.success(calibrationJob.calibrate()));
    }
}
