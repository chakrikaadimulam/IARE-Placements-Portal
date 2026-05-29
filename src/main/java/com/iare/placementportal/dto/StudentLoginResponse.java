package com.iare.placementportal.dto;

public record StudentLoginResponse(
        boolean success,
        String message,
        Long studentId,
        String rollNo,
        String studentName,
        String branch,
        Integer semester,
        String section,
        String photoUrl
) {
}
