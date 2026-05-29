package com.iare.placementportal.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record InterviewExperienceResponse(
        Long id,
        Long placementDriveId,
        String driveTitle,
        Integer hiringYear,
        LocalDate hiringDate,
        Long companyId,
        String companyName,
        String companyLogoUrl,
        String companyWebsiteUrl,
        String roleOffered,
        String studentName,
        String studentPhotoUrl,
        String difficultyLevel,
        String roundsFaced,
        String questionsAsked,
        String codingQuestions,
        String technicalTopics,
        String hrQuestions,
        String preparationTips,
        String finalResult,
        LocalDate experienceDate,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
