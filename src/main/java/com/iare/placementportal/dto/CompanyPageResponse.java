package com.iare.placementportal.dto;

import java.util.List;

public record CompanyPageResponse(
        List<CompanyListDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
