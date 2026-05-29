package com.iare.placementportal.dto;

import java.util.List;

public record StudentExcelUploadResponse(
        int totalRows,
        int insertedCount,
        int updatedCount,
        int skippedCount,
        List<String> errors
) {
}
