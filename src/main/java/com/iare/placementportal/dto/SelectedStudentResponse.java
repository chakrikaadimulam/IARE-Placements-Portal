package com.iare.placementportal.dto;

import java.time.LocalDateTime;

public record SelectedStudentResponse(
        Long id,
        Long placementDriveId,
        String driveTitle,
        Integer hiringYear,
        Long companyId,
        String companyName,
        String companyLogoUrl,
        String studentName,
        String rollNumber,
        String branch,
        String gender,
        String photoUrl,
        String packageOffered,
        String offerType,
        Integer selectionYear,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
