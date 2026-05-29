package com.iare.placementportal.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PlacementDriveResponse(
        Long id,
        Long companyId,
        String companyName,
        String companyLogoUrl,
        String companyWebsiteUrl,
        String companyType,
        String industry,
        String driveTitle,
        Integer hiringYear,
        LocalDate hiringDate,
        String hiringMode,
        String hiringLocation,
        String eligibleBranches,
        Double eligibleCgpa,
        Boolean backlogsAllowed,
        Integer maxBacklogs,
        String bondDetails,
        String jobType,
        String ctcPackage,
        String stipend,
        Integer numberOfRounds,
        String roundNames,
        LocalDate registrationDeadline,
        LocalDate examDate,
        LocalDate interviewDate,
        String driveStatus,
        String description,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
