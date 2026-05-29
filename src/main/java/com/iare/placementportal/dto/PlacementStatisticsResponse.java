package com.iare.placementportal.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PlacementStatisticsResponse(
        Long id,
        Long placementDriveId,
        String driveTitle,
        Integer hiringYear,
        LocalDate hiringDate,
        String driveStatus,
        Long companyId,
        String companyName,
        String companyLogoUrl,
        String companyWebsiteUrl,
        String companyType,
        String industry,
        Integer studentsApplied,
        Integer studentsAttended,
        Integer studentsShortlisted,
        Integer studentsSelected,
        Integer maleSelected,
        Integer femaleSelected,
        Double highestPackage,
        Double averagePackage,
        Double lowestPackage,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
