package com.iare.placementportal.controller;

import com.iare.placementportal.dto.CompanyRequest;
import com.iare.placementportal.dto.CompanyResponse;
import com.iare.placementportal.service.CompanyService;
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

    @GetMapping("/api/admin/companies")
    public List<CompanyResponse> getAllCompanies() {
        return companyService.getAllCompanies();
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
}
