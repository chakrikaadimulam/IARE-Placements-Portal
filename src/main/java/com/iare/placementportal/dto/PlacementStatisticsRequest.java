package com.iare.placementportal.dto;

import jakarta.validation.constraints.NotNull;

public record PlacementStatisticsRequest(
        @NotNull(message = "Placement Drive is required.")
        Long placementDriveId,
        Integer studentsApplied,
        Integer studentsAttended,
        Integer studentsShortlisted,
        Integer studentsSelected,
        Integer maleSelected,
        Integer femaleSelected,
        Double highestPackage,
        Double averagePackage,
        Double lowestPackage
) {
}
