package com.iare.placementportal.controller;

import com.iare.placementportal.dto.ApiErrorResponse;
import com.iare.placementportal.dto.PlacementDriveExcelUploadResponse;
import com.iare.placementportal.dto.PlacementDriveFilterOptionsResponse;
import com.iare.placementportal.dto.PlacementDrivePageResponse;
import com.iare.placementportal.dto.PlacementDriveRequest;
import com.iare.placementportal.dto.PlacementDriveResponse;
import com.iare.placementportal.service.PlacementDriveService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    @PostMapping(
            value = {"/api/admin/placement-drives/upload", "/admin/placement-drives/upload"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadPlacementDrives(@RequestParam("file") MultipartFile file) {
        try {
            PlacementDriveExcelUploadResponse response = placementDriveService.uploadDrivesFromExcel(file);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException exception) {
            HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
            return ResponseEntity.status(status)
                    .body(new ApiErrorResponse(exception.getReason(), null, LocalDateTime.now()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse(
                            "Unable to upload placement drive Excel file.",
                            exception.getMessage(),
                            LocalDateTime.now()
                    ));
        }
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

    @GetMapping("/api/placement-drives")
    public ResponseEntity<?> getActiveDrivesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Integer hiringYear,
            @RequestParam(defaultValue = "") String driveStatus,
            @RequestParam(defaultValue = "") String jobType) {
        try {
            PlacementDrivePageResponse response = placementDriveService.getActiveDrivesPaginated(
                    page, size, search, hiringYear, driveStatus, jobType
            );
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                    .body(response);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse(
                            "Unable to load placement drives.",
                            exception.getMessage(),
                            LocalDateTime.now()
                    ));
        }
    }

    @GetMapping("/api/placement-drives/filter-options")
    public ResponseEntity<PlacementDriveFilterOptionsResponse> getPlacementDriveFilterOptions() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(placementDriveService.getStudentDriveFilterOptions());
    }

    @GetMapping("/api/student/placement-drives/company/{companyId}")
    public List<PlacementDriveResponse> getActiveDrivesByCompany(@PathVariable Long companyId) {
        return placementDriveService.getActiveDrivesByCompany(companyId);
    }
}
