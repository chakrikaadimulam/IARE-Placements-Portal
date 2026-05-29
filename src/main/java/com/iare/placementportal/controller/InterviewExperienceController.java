package com.iare.placementportal.controller;

import com.iare.placementportal.dto.InterviewExperienceRequest;
import com.iare.placementportal.dto.InterviewExperienceResponse;
import com.iare.placementportal.service.InterviewExperienceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
public class InterviewExperienceController {

    private final InterviewExperienceService interviewExperienceService;

    public InterviewExperienceController(InterviewExperienceService interviewExperienceService) {
        this.interviewExperienceService = interviewExperienceService;
    }

    @PostMapping("/api/admin/interview-experiences")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewExperienceResponse createExperience(@Valid @RequestBody InterviewExperienceRequest request) {
        return interviewExperienceService.createExperience(request);
    }

    @GetMapping("/api/admin/interview-experiences")
    public List<InterviewExperienceResponse> getAllExperiences() {
        return interviewExperienceService.getAllExperiences();
    }

    @GetMapping("/api/admin/interview-experiences/drive/{placementDriveId}")
    public List<InterviewExperienceResponse> getExperiencesByDrive(@PathVariable Long placementDriveId) {
        return interviewExperienceService.getExperiencesByDrive(placementDriveId);
    }

    @PutMapping("/api/admin/interview-experiences/{id}")
    public InterviewExperienceResponse updateExperience(@PathVariable Long id,
                                                        @Valid @RequestBody InterviewExperienceRequest request) {
        return interviewExperienceService.updateExperience(id, request);
    }

    @DeleteMapping("/api/admin/interview-experiences/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExperience(@PathVariable Long id) {
        interviewExperienceService.deleteExperience(id);
    }

    @PatchMapping("/api/admin/interview-experiences/{id}/status")
    public InterviewExperienceResponse changeExperienceStatus(@PathVariable Long id, @RequestParam boolean active) {
        return interviewExperienceService.changeExperienceActiveStatus(id, active);
    }

    @GetMapping("/api/student/interview-experiences")
    public List<InterviewExperienceResponse> getActiveExperiences() {
        return interviewExperienceService.getActiveExperiencesForStudents();
    }

    @GetMapping("/api/student/interview-experiences/drive/{placementDriveId}")
    public List<InterviewExperienceResponse> getActiveExperiencesByDrive(@PathVariable Long placementDriveId) {
        return interviewExperienceService.getActiveExperiencesByDrive(placementDriveId);
    }
}
