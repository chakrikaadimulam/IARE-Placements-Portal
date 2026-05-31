package com.iare.placementportal.controller;

import com.iare.placementportal.dto.ApiErrorResponse;
import com.iare.placementportal.dto.CompanyExcelUploadResponse;
import com.iare.placementportal.dto.CompanyPageResponse;
import com.iare.placementportal.dto.CompanyRequest;
import com.iare.placementportal.dto.CompanyResponse;
import com.iare.placementportal.service.CompanyService;
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
import java.util.concurrent.TimeUnit;
import java.util.List;

@RestController
@RequestMapping
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/api/admin/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse createCompany(@Valid @RequestBody CompanyRequest request) {
        return companyService.createCompany(request);
    }

    @PostMapping(value = "/api/admin/companies/upload-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCompaniesExcel(@RequestParam("file") MultipartFile file) {
        try {
            CompanyExcelUploadResponse response = companyService.uploadCompaniesFromExcel(file);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException exception) {
            HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
            return ResponseEntity.status(status)
                    .body(new ApiErrorResponse(exception.getReason(), null, LocalDateTime.now()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse(
                            "Unable to upload company Excel file.",
                            exception.getMessage(),
                            LocalDateTime.now()
                    ));
        }
    }

    @GetMapping("/api/admin/companies")
    public List<CompanyResponse> getAllCompanies() {
        return companyService.getAllCompanies();
    }

    @GetMapping("/api/admin/companies/paged")
    public ResponseEntity<?> getAdminCompaniesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search) {
        try {
            CompanyPageResponse response = companyService.getAdminCompaniesPaginated(page, size, search);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .body(response);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse(
                            "Unable to load companies.",
                            exception.getMessage(),
                            LocalDateTime.now()
                    ));
        }
    }

    @PutMapping("/api/admin/companies/{id}")
    public CompanyResponse updateCompany(@PathVariable Long id, @Valid @RequestBody CompanyRequest request) {
        return companyService.updateCompany(id, request);
    }

    @DeleteMapping("/api/admin/companies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
    }

    @PatchMapping("/api/admin/companies/{id}/status")
    public CompanyResponse changeCompanyStatus(@PathVariable Long id, @RequestParam boolean active) {
        return companyService.changeCompanyActiveStatus(id, active);
    }

    @GetMapping("/api/student/companies")
    public List<CompanyResponse> getActiveCompanies() {
        return companyService.getActiveCompaniesForStudents();
    }

    @GetMapping("/api/companies")
    public ResponseEntity<?> getCompaniesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search) {
        try {
            CompanyPageResponse response = companyService.getCompaniesPaginated(page, size, search);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                    .body(response);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse(
                            "Unable to load companies.",
                            exception.getMessage(),
                            LocalDateTime.now()
                    ));
        }
    }
}
