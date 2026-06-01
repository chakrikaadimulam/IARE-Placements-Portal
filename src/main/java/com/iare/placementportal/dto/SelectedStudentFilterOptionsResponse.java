package com.iare.placementportal.dto;

import java.util.List;

public record SelectedStudentFilterOptionsResponse(
        List<String> branches,
        List<String> companies
) {
}
