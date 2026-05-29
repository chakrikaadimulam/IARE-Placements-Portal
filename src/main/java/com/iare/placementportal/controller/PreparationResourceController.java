package com.iare.placementportal.controller;

import com.iare.placementportal.dto.PreparationResourceResponse;
import com.iare.placementportal.dto.PreparationResourceStudentResponse;
import com.iare.placementportal.service.PreparationResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping
public class PreparationResourceController {

    private final PreparationResourceService preparationResourceService;

    public PreparationResourceController(PreparationResourceService preparationResourceService) {
        this.preparationResourceService = preparationResourceService;
    }

    @PostMapping(value = "/api/admin/preparation-resources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PreparationResourceResponse createResource(@RequestParam Long placementDriveId,
                                                      @RequestParam String resourceTitle,
                                                      @RequestParam(required = false) String description,
                                                      @RequestParam(required = false) MultipartFile aptitudePdf,
                                                      @RequestParam(required = false) MultipartFile codingPdf,
                                                      @RequestParam(required = false) MultipartFile technicalPdf,
                                                      @RequestParam(required = false) MultipartFile hrPdf) {
        return preparationResourceService.createResource(
                placementDriveId,
                resourceTitle,
                description,
                aptitudePdf,
                codingPdf,
                technicalPdf,
                hrPdf
        );
    }

    @GetMapping("/api/admin/preparation-resources")
    public List<PreparationResourceResponse> getAllResources() {
        return preparationResourceService.getAllResources();
    }

    @GetMapping("/api/admin/preparation-resources/drive/{placementDriveId}")
    public List<PreparationResourceResponse> getResourcesByDrive(@PathVariable Long placementDriveId) {
        return preparationResourceService.getResourcesByDrive(placementDriveId);
    }

    @GetMapping("/api/admin/preparation-resources/{id}/pdf/{type}/view")
    public ResponseEntity<byte[]> viewAdminPdf(@PathVariable Long id, @PathVariable String type) {
        return preparationResourceService.getAdminPdfResponse(id, type, false);
    }

    @GetMapping("/api/admin/preparation-resources/{id}/pdf/{type}/download")
    public ResponseEntity<byte[]> downloadAdminPdf(@PathVariable Long id, @PathVariable String type) {
        return preparationResourceService.getAdminPdfResponse(id, type, true);
    }

    @PutMapping(value = "/api/admin/preparation-resources/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PreparationResourceResponse updateResource(@PathVariable Long id,
                                                      @RequestParam Long placementDriveId,
                                                      @RequestParam String resourceTitle,
                                                      @RequestParam(required = false) String description,
                                                      @RequestParam(required = false) MultipartFile aptitudePdf,
                                                      @RequestParam(required = false) MultipartFile codingPdf,
                                                      @RequestParam(required = false) MultipartFile technicalPdf,
                                                      @RequestParam(required = false) MultipartFile hrPdf) {
        return preparationResourceService.updateResource(
                id,
                placementDriveId,
                resourceTitle,
                description,
                aptitudePdf,
                codingPdf,
                technicalPdf,
                hrPdf
        );
    }

    @DeleteMapping("/api/admin/preparation-resources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@PathVariable Long id) {
        preparationResourceService.deleteResource(id);
    }

    @PatchMapping("/api/admin/preparation-resources/{id}/status")
    public PreparationResourceResponse changeResourceStatus(@PathVariable Long id, @RequestParam boolean active) {
        return preparationResourceService.changeResourceActiveStatus(id, active);
    }

    @GetMapping("/api/student/preparation-resources")
    public List<PreparationResourceStudentResponse> getActiveResources() {
        return preparationResourceService.getActiveResourcesForStudents();
    }

    @GetMapping("/api/student/preparation-resources/drive/{placementDriveId}")
    public List<PreparationResourceStudentResponse> getActiveResourcesByDrive(@PathVariable Long placementDriveId) {
        return preparationResourceService.getActiveResourcesByDrive(placementDriveId);
    }

    @GetMapping("/api/student/preparation-resources/{id}/pdf/{type}/view")
    public ResponseEntity<byte[]> viewStudentPdf(@PathVariable Long id, @PathVariable String type) {
        return preparationResourceService.getStudentPdfResponse(id, type, false);
    }

    @GetMapping("/api/student/preparation-resources/{id}/pdf/{type}/download")
    public ResponseEntity<byte[]> downloadStudentPdf(@PathVariable Long id, @PathVariable String type) {
        return preparationResourceService.getStudentPdfResponse(id, type, true);
    }
}
