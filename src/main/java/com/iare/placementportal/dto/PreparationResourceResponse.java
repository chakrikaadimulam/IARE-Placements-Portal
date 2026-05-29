package com.iare.placementportal.dto;

import java.time.LocalDateTime;

public record PreparationResourceResponse(
        Long id,
        Long placementDriveId,
        String driveTitle,
        Integer hiringYear,
        Long companyId,
        String companyName,
        String companyLogoUrl,
        String companyWebsiteUrl,
        String resourceTitle,
        String description,
        String aptitudePdfUrl,
        String codingPdfUrl,
        String technicalPdfUrl,
        String hrPdfUrl,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
