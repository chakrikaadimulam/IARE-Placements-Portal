package com.iare.placementportal.service;

import com.iare.placementportal.dto.CompanyRequest;
import com.iare.placementportal.dto.CompanyResponse;
import com.iare.placementportal.entity.Company;
import com.iare.placementportal.repository.CompanyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Year;
import java.util.List;

@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public CompanyResponse createCompany(CompanyRequest request) {
        validateRequest(request);

        if (companyRepository.existsByCompanyNameIgnoreCase(request.companyName().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A company with this name already exists.");
        }

        Company company = new Company();
        mapRequestToEntity(request, company);

        return toResponse(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getActiveCompaniesForStudents() {
        return companyRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CompanyResponse updateCompany(Long id, CompanyRequest request) {
        validateRequest(request);

        Company company = findCompanyOrThrow(id);
        companyRepository.findByCompanyNameIgnoreCase(request.companyName().trim())
                .filter(existingCompany -> !existingCompany.getId().equals(id))
                .ifPresent(existingCompany -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A company with this name already exists.");
                });

        mapRequestToEntity(request, company);
        return toResponse(companyRepository.save(company));
    }

    public void deleteCompany(Long id) {
        Company company = findCompanyOrThrow(id);
        companyRepository.delete(company);
    }

    public CompanyResponse changeCompanyActiveStatus(Long id, boolean active) {
        Company company = findCompanyOrThrow(id);
        company.setActive(active);
        return toResponse(companyRepository.save(company));
    }

    private Company findCompanyOrThrow(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found."));
    }

    private void validateRequest(CompanyRequest request) {
        if (request.foundedYear() != null && request.foundedYear() > Year.now().getValue()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Founded Year cannot be in the future.");
        }

        validateUrl(request.logoUrl(), "Company Logo URL must be a valid URL.");
        validateUrl(request.websiteUrl(), "Company Website URL must be a valid URL.");
    }

    private void mapRequestToEntity(CompanyRequest request, Company company) {
        company.setCompanyName(request.companyName().trim());
        company.setLogoUrl(normalizeOptional(request.logoUrl()));
        company.setWebsiteUrl(normalizeOptional(request.websiteUrl()));
        company.setCompanyType(request.companyType().trim());
        company.setIndustry(request.industry().trim());
        company.setHeadquarters(normalizeOptional(request.headquarters()));
        company.setFoundedYear(request.foundedYear());
        company.setDescription(request.description().trim());
        if (company.getActive() == null) {
            company.setActive(true);
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateUrl(String value, String message) {
        String normalizedValue = normalizeOptional(value);
        if (normalizedValue == null) {
            return;
        }

        try {
            URI uri = new URI(normalizedValue);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
            }
        } catch (URISyntaxException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getCompanyName(),
                company.getLogoUrl(),
                company.getWebsiteUrl(),
                company.getCompanyType(),
                company.getIndustry(),
                company.getHeadquarters(),
                company.getFoundedYear(),
                company.getDescription(),
                company.getActive(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}
