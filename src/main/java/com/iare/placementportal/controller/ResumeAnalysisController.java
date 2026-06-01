package com.iare.placementportal.controller;

import com.iare.placementportal.dto.ApiErrorResponse;
import com.iare.placementportal.dto.ResumeAnalysisResponse;
import com.iare.placementportal.service.ResumeAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/resume")
public class ResumeAnalysisController {

    private final ResumeAnalysisService resumeAnalysisService;

    public ResumeAnalysisController(ResumeAnalysisService resumeAnalysisService) {
        this.resumeAnalysisService = resumeAnalysisService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzeResume(@RequestParam("resume") MultipartFile resumeFile) {
        try {
            ResumeAnalysisResponse response = resumeAnalysisService.analyzeResume(resumeFile);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException exception) {
            HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
            return ResponseEntity.status(status)
                    .body(new ApiErrorResponse(exception.getReason(), null, LocalDateTime.now()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse(
                            "Unable to analyze resume.",
                            exception.getMessage(),
                            LocalDateTime.now()
                    ));
        }
    }
}
