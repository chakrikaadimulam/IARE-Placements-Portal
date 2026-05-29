package com.iare.placementportal.controller;

import com.iare.placementportal.dto.PlacementDriveRequest;
import com.iare.placementportal.dto.PlacementDriveResponse;
import com.iare.placementportal.service.PlacementDriveService;
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
public class PlacementDriveController {

    private final PlacementDriveService placementDriveService;

    public PlacementDriveController(PlacementDriveService placementDriveService) {
        this.placementDriveService = placementDriveService;
    }

    @PostMapping("/api/admin/placement-drives")
    @ResponseStatus(HttpStatus.CREATED)
    public PlacementDriveResponse createDrive(@Valid @RequestBody PlacementDriveRequest request) {
        return placementDriveService.createDrive(request);
    }

    @GetMapping("/api/admin/placement-drives")
    public List<PlacementDriveResponse> getAllDrives() {
        return placementDriveService.getAllDrives();
    }

    @GetMapping("/api/admin/placement-drives/company/{companyId}")
    public List<PlacementDriveResponse> getDrivesByCompany(@PathVariable Long companyId) {
        return placementDriveService.getDrivesByCompany(companyId);
    }

    @PutMapping("/api/admin/placement-drives/{id}")
    public PlacementDriveResponse updateDrive(@PathVariable Long id, @Valid @RequestBody PlacementDriveRequest request) {
        return placementDriveService.updateDrive(id, request);
    }

    @DeleteMapping("/api/admin/placement-drives/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDrive(@PathVariable Long id) {
        placementDriveService.deleteDrive(id);
    }

    @PatchMapping("/api/admin/placement-drives/{id}/status")
    public PlacementDriveResponse changeDriveStatus(@PathVariable Long id, @RequestParam boolean active) {
        return placementDriveService.changeDriveActiveStatus(id, active);
    }

    @GetMapping("/api/student/placement-drives")
    public List<PlacementDriveResponse> getActiveDrives() {
        return placementDriveService.getActiveDrivesForStudents();
    }

    @GetMapping("/api/student/placement-drives/company/{companyId}")
    public List<PlacementDriveResponse> getActiveDrivesByCompany(@PathVariable Long companyId) {
        return placementDriveService.getActiveDrivesByCompany(companyId);
    }
}
