package com.iare.placementportal.dto;

public record PreparationResourceStudentResponse(
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
        Boolean hasAptitudePdf,
        Boolean hasCodingPdf,
        Boolean hasTechnicalPdf,
        Boolean hasHrPdf
) {
}
