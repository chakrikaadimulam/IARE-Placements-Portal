package com.iare.placementportal.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NoticeResponse(
        Long id,
        String title,
        String message,
        LocalDate validFrom,
        LocalDate validTo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean active,
        String status
) {
}
