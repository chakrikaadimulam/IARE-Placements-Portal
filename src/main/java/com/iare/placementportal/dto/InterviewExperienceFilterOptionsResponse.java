package com.iare.placementportal.dto;

import java.util.List;

public record InterviewExperienceFilterOptionsResponse(
        List<FilterOptionResponse> drives,
        List<Integer> hiringYears,
        List<String> finalResults
) {
}
