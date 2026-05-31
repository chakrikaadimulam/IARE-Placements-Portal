package com.iare.placementportal.dto;

import java.util.List;

public record CompanyExcelUploadResponse(
        int totalRows,
        int insertedCount,
        int updatedCount,
        int skippedCount,
        List<CompanyExcelUploadError> errors
) {
}
