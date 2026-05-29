package com.iare.placementportal.dto;

import java.time.LocalDateTime;

public record CompanyResponse(
        Long id,
        String companyName,
        String logoUrl,
        String websiteUrl,
        String companyType,
        String industry,
        String headquarters,
        Integer foundedYear,
        String description,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
