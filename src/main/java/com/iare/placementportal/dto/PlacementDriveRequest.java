package com.iare.placementportal.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PlacementDriveRequest(
        @NotNull(message = "Company is required.")
        Long companyId,
        @NotBlank(message = "Drive Title is required.")
        String driveTitle,
        @NotNull(message = "Hiring Year is required.")
        Integer hiringYear,
        @NotNull(message = "Hiring Date is required.")
        LocalDate hiringDate,
        @NotBlank(message = "Hiring Mode is required.")
        String hiringMode,
        String hiringLocation,
        @NotBlank(message = "Eligible Branches is required.")
        String eligibleBranches,
        @NotNull(message = "Eligible CGPA is required.")
        @DecimalMin(value = "0.0", message = "Eligible CGPA must be between 0 and 10.")
        @DecimalMax(value = "10.0", message = "Eligible CGPA must be between 0 and 10.")
        Double eligibleCgpa,
        Boolean backlogsAllowed,
        Integer maxBacklogs,
        String bondDetails,
        @NotBlank(message = "Job Type is required.")
        String jobType,
        @NotBlank(message = "CTC Package is required.")
        String ctcPackage,
        String stipend,
        Integer numberOfRounds,
        String roundNames,
        LocalDate registrationDeadline,
        LocalDate examDate,
        LocalDate interviewDate,
        @NotBlank(message = "Drive Status is required.")
        String driveStatus,
        String description
) {
}
