package com.iare.placementportal.dto;

import java.util.List;

public record PlacementDriveFilterOptionsResponse(
        List<Integer> hiringYears,
        List<String> jobTypes
) {
}
