package com.iare.placementportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record InterviewExperienceRequest(
        @NotNull(message = "Placement Drive is required.")
        Long placementDriveId,
        @NotBlank(message = "Student Name is required.")
        String studentName,
        String studentPhotoUrl,
        @NotBlank(message = "Role Offered is required.")
        String roleOffered,
        @NotBlank(message = "Difficulty Level is required.")
        String difficultyLevel,
        @NotBlank(message = "Rounds Faced is required.")
        String roundsFaced,
        @NotBlank(message = "Questions Asked is required.")
        String questionsAsked,
        String codingQuestions,
        String technicalTopics,
        String hrQuestions,
        @NotBlank(message = "Preparation Tips is required.")
        String preparationTips,
        @NotBlank(message = "Final Result is required.")
        String finalResult,
        @NotNull(message = "Experience Date is required.")
        LocalDate experienceDate
) {
}
