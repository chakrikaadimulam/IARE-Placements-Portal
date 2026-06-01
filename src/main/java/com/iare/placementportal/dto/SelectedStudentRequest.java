package com.iare.placementportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SelectedStudentRequest(
        @NotNull(message = "Placement Drive is required.")
        Long placementDriveId,
        @NotBlank(message = "Student Name is required.")
        String studentName,
        @NotBlank(message = "Roll Number is required.")
        String rollNumber,
        @NotBlank(message = "Branch is required.")
        String branch,
        @NotBlank(message = "Gender is required.")
        String gender,
        String photoUrl,
        @NotBlank(message = "Package Offered is required.")
        String packageOffered,
        @NotBlank(message = "Offer Type is required.")
        String offerType,
        @NotNull(message = "Selection Year is required.")
        Integer selectionYear
) {
}
