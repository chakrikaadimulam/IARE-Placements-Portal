package com.iare.placementportal.controller;

import com.iare.placementportal.dto.PagedResponse;
import com.iare.placementportal.dto.PlacementStatisticsFilterOptionsResponse;
import com.iare.placementportal.dto.PlacementStatisticsRequest;
import com.iare.placementportal.dto.PlacementStatisticsResponse;
import com.iare.placementportal.service.PlacementStatisticsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
public class PlacementStatisticsController {

    private final PlacementStatisticsService placementStatisticsService;

    public PlacementStatisticsController(PlacementStatisticsService placementStatisticsService) {
        this.placementStatisticsService = placementStatisticsService;
    }

    @PostMapping("/api/admin/placement-statistics")
    @ResponseStatus(HttpStatus.CREATED)
    public PlacementStatisticsResponse createStatistics(@Valid @RequestBody PlacementStatisticsRequest request) {
        return placementStatisticsService.createStatistics(request);
    }

    @GetMapping("/api/admin/placement-statistics")
    public List<PlacementStatisticsResponse> getAllStatistics() {
        return placementStatisticsService.getAllStatistics();
    }

    @GetMapping("/api/admin/placement-statistics/drive/{placementDriveId}")
    public PlacementStatisticsResponse getStatisticsByDrive(@PathVariable Long placementDriveId) {
        return placementStatisticsService.getStatisticsByDrive(placementDriveId);
    }

    @PutMapping("/api/admin/placement-statistics/{id}")
    public PlacementStatisticsResponse updateStatistics(@PathVariable Long id,
                                                        @Valid @RequestBody PlacementStatisticsRequest request) {
        return placementStatisticsService.updateStatistics(id, request);
    }

    @DeleteMapping("/api/admin/placement-statistics/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStatistics(@PathVariable Long id) {
        placementStatisticsService.deleteStatistics(id);
    }

    @PatchMapping("/api/admin/placement-statistics/{id}/status")
    public PlacementStatisticsResponse changeStatisticsStatus(@PathVariable Long id, @RequestParam boolean active) {
        return placementStatisticsService.changeStatisticsActiveStatus(id, active);
    }

    @GetMapping("/api/student/placement-statistics")
    public List<PlacementStatisticsResponse> getActiveStatistics() {
        return placementStatisticsService.getActiveStatisticsForStudents();
    }

    @GetMapping("/api/student/placement-statistics/paged")
    public PagedResponse<PlacementStatisticsResponse> getActiveStatisticsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Integer hiringYear,
            @RequestParam(defaultValue = "") String driveStatus
    ) {
        return placementStatisticsService.getActiveStatisticsPageForStudents(page, size, search, hiringYear, driveStatus);
    }

    @GetMapping("/api/student/placement-statistics/filter-options")
    public PlacementStatisticsFilterOptionsResponse getActiveStatisticsFilterOptions() {
        return placementStatisticsService.getActiveStatisticsFilterOptions();
    }
}
