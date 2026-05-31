package com.iare.placementportal.dto;

public record PlacementDriveExcelUploadError(
        int rowNumber,
        String companyName,
        String reason
) {
}
