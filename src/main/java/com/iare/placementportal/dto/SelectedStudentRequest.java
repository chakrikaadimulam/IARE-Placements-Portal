package com.iare.placementportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SelectedStudentRequest(
        @NotNull(message = "Placement Drive is required.")
        Long placementDriveId,
        @NotBlank(message = "Student Name is required.")
        String studentName,
        @NotBlank(message = "Roll Number is required.")
        String rollNumber,
        @NotBlank(message = "Branch is required.")
        String branch,
        @NotBlank(message = "Section is required.")
        String section,
        @NotBlank(message = "Gender is required.")
        String gender,
        String photoUrl,
        @NotBlank(message = "Package Offered is required.")
        String packageOffered,
        @NotBlank(message = "Role Offered is required.")
        String roleOffered,
        @NotBlank(message = "Offer Type is required.")
        String offerType,
        @NotNull(message = "Selection Date is required.")
        LocalDate selectionDate
) {
}
