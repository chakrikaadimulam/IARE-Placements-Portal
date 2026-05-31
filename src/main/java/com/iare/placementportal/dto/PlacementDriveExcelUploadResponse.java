package com.iare.placementportal.dto;

import java.util.List;

public record PlacementDriveExcelUploadResponse(
        int totalRows,
        int successCount,
        int failedCount,
        List<PlacementDriveExcelUploadError> errors
) {
}
