package com.iare.placementportal.dto;

import java.util.List;

public record PlacementDrivePageResponse(
        List<PlacementDriveResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
