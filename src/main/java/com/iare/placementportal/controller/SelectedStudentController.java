package com.iare.placementportal.controller;

import com.iare.placementportal.dto.PagedResponse;
import com.iare.placementportal.dto.SelectedStudentRequest;
import com.iare.placementportal.dto.SelectedStudentFilterOptionsResponse;
import com.iare.placementportal.dto.SelectedStudentResponse;
import com.iare.placementportal.service.SelectedStudentService;
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
public class SelectedStudentController {

    private final SelectedStudentService selectedStudentService;

    public SelectedStudentController(SelectedStudentService selectedStudentService) {
        this.selectedStudentService = selectedStudentService;
    }

    @PostMapping("/api/admin/selected-students")
    @ResponseStatus(HttpStatus.CREATED)
    public SelectedStudentResponse createSelectedStudent(@Valid @RequestBody SelectedStudentRequest request) {
        return selectedStudentService.createSelectedStudent(request);
    }

    @GetMapping("/api/admin/selected-students")
    public List<SelectedStudentResponse> getAllSelectedStudents() {
        return selectedStudentService.getAllSelectedStudents();
    }

    @GetMapping("/api/admin/selected-students/drive/{placementDriveId}")
    public List<SelectedStudentResponse> getSelectedStudentsByDrive(@PathVariable Long placementDriveId) {
        return selectedStudentService.getSelectedStudentsByDrive(placementDriveId);
    }

    @PutMapping("/api/admin/selected-students/{id}")
    public SelectedStudentResponse updateSelectedStudent(@PathVariable Long id,
                                                         @Valid @RequestBody SelectedStudentRequest request) {
        return selectedStudentService.updateSelectedStudent(id, request);
    }

    @DeleteMapping("/api/admin/selected-students/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSelectedStudent(@PathVariable Long id) {
        selectedStudentService.deleteSelectedStudent(id);
    }

    @PatchMapping("/api/admin/selected-students/{id}/status")
    public SelectedStudentResponse changeSelectedStudentStatus(@PathVariable Long id, @RequestParam boolean active) {
        return selectedStudentService.changeSelectedStudentActiveStatus(id, active);
    }

    @GetMapping("/api/student/selected-students")
    public List<SelectedStudentResponse> getActiveSelectedStudents() {
        return selectedStudentService.getActiveSelectedStudentsForStudents();
    }

    @GetMapping("/api/student/selected-students/paged")
    public PagedResponse<SelectedStudentResponse> getActiveSelectedStudentsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String branch,
            @RequestParam(defaultValue = "") String company
    ) {
        return selectedStudentService.getActiveSelectedStudentsPageForStudents(page, size, search, branch, company);
    }

    @GetMapping("/api/student/selected-students/filter-options")
    public SelectedStudentFilterOptionsResponse getActiveSelectedStudentFilterOptions() {
        return selectedStudentService.getActiveSelectedStudentFilterOptions();
    }

    @GetMapping("/api/student/selected-students/drive/{placementDriveId}")
    public List<SelectedStudentResponse> getActiveSelectedStudentsByDrive(@PathVariable Long placementDriveId) {
        return selectedStudentService.getActiveSelectedStudentsByDrive(placementDriveId);
    }
}
