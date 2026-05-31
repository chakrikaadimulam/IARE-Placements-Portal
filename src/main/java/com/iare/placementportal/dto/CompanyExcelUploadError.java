package com.iare.placementportal.dto;

public record CompanyExcelUploadError(
        int rowNumber,
        String reason
) {
}
