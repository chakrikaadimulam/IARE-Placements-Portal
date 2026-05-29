package com.iare.placementportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record NoticeRequest(
        @NotBlank(message = "Title is required.")
        String title,
        @NotBlank(message = "Message is required.")
        String message,
        @NotNull(message = "Valid From Date is required.")
        LocalDate validFrom,
        @NotNull(message = "Valid To Date is required.")
        LocalDate validTo
) {
}
