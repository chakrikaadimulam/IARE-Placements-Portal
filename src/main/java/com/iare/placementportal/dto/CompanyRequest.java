package com.iare.placementportal.dto;

import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(
        @NotBlank(message = "Company Name is required.")
        String companyName,
        String logoUrl,
        String websiteUrl,
        @NotBlank(message = "Company Type is required.")
        String companyType,
        @NotBlank(message = "Industry is required.")
        String industry,
        String headquarters,
        Integer foundedYear,
        @NotBlank(message = "Company Description is required.")
        String description
) {
}
