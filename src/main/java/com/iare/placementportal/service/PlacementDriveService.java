package com.iare.placementportal.service;

import com.iare.placementportal.dto.PlacementDriveRequest;
import com.iare.placementportal.dto.PlacementDriveResponse;
import com.iare.placementportal.entity.Company;
import com.iare.placementportal.entity.PlacementDrive;
import com.iare.placementportal.repository.CompanyRepository;
import com.iare.placementportal.repository.PlacementDriveRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class PlacementDriveService {

    private final PlacementDriveRepository placementDriveRepository;
    private final CompanyRepository companyRepository;

    public PlacementDriveService(PlacementDriveRepository placementDriveRepository, CompanyRepository companyRepository) {
        this.placementDriveRepository = placementDriveRepository;
        this.companyRepository = companyRepository;
    }

    public PlacementDriveResponse createDrive(PlacementDriveRequest request) {
        validateRequest(request);
        Company company = findCompanyOrThrow(request.companyId());

        PlacementDrive placementDrive = new PlacementDrive();
        mapRequestToEntity(request, placementDrive, company);

        return toResponse(placementDriveRepository.save(placementDrive));
    }

    @Transactional(readOnly = true)
    public List<PlacementDriveResponse> getAllDrives() {
        return placementDriveRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlacementDriveResponse> getActiveDrivesForStudents() {
        return placementDriveRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlacementDriveResponse> getDrivesByCompany(Long companyId) {
        findCompanyOrThrow(companyId);
        return placementDriveRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlacementDriveResponse> getActiveDrivesByCompany(Long companyId) {
        findCompanyOrThrow(companyId);
        return placementDriveRepository.findByCompanyIdAndActiveTrueOrderByCreatedAtDesc(companyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PlacementDriveResponse updateDrive(Long id, PlacementDriveRequest request) {
        validateRequest(request);
        Company company = findCompanyOrThrow(request.companyId());
        PlacementDrive placementDrive = findDriveOrThrow(id);

        mapRequestToEntity(request, placementDrive, company);

        return toResponse(placementDriveRepository.save(placementDrive));
    }

    public void deleteDrive(Long id) {
        PlacementDrive placementDrive = findDriveOrThrow(id);
        placementDriveRepository.delete(placementDrive);
    }

    public PlacementDriveResponse changeDriveActiveStatus(Long id, boolean active) {
        PlacementDrive placementDrive = findDriveOrThrow(id);
        placementDrive.setActive(active);
        return toResponse(placementDriveRepository.save(placementDrive));
    }

    private Company findCompanyOrThrow(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected company does not exist."));
    }

    private PlacementDrive findDriveOrThrow(Long id) {
        return placementDriveRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Placement drive not found."));
    }

    private void validateRequest(PlacementDriveRequest request) {
        if (request.eligibleCgpa() != null && (request.eligibleCgpa() < 0 || request.eligibleCgpa() > 10)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Eligible CGPA must be between 0 and 10.");
        }
        if (request.maxBacklogs() != null && request.maxBacklogs() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum Backlogs cannot be negative.");
        }
        if (request.numberOfRounds() != null && request.numberOfRounds() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Number of Rounds cannot be negative.");
        }
        if (request.registrationDeadline() != null
                && request.hiringDate() != null
                && request.registrationDeadline().isAfter(request.hiringDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration Deadline cannot be after Hiring Date.");
        }
    }

    private void mapRequestToEntity(PlacementDriveRequest request, PlacementDrive placementDrive, Company company) {
        placementDrive.setCompany(company);
        placementDrive.setDriveTitle(request.driveTitle().trim());
        placementDrive.setHiringYear(request.hiringYear());
        placementDrive.setHiringDate(request.hiringDate());
        placementDrive.setHiringMode(request.hiringMode().trim());
        placementDrive.setHiringLocation(normalizeOptional(request.hiringLocation()));
        placementDrive.setEligibleBranches(request.eligibleBranches().trim());
        placementDrive.setEligibleCgpa(request.eligibleCgpa());
        placementDrive.setBacklogsAllowed(Boolean.TRUE.equals(request.backlogsAllowed()));
        placementDrive.setMaxBacklogs(request.maxBacklogs());
        placementDrive.setBondDetails(normalizeOptional(request.bondDetails()));
        placementDrive.setJobType(request.jobType().trim());
        placementDrive.setCtcPackage(request.ctcPackage().trim());
        placementDrive.setStipend(normalizeOptional(request.stipend()));
        placementDrive.setNumberOfRounds(request.numberOfRounds());
        placementDrive.setRoundNames(normalizeOptional(request.roundNames()));
        placementDrive.setRegistrationDeadline(request.registrationDeadline());
        placementDrive.setExamDate(request.examDate());
        placementDrive.setInterviewDate(request.interviewDate());
        placementDrive.setDriveStatus(request.driveStatus().trim());
        placementDrive.setDescription(normalizeOptional(request.description()));
        if (placementDrive.getActive() == null) {
            placementDrive.setActive(true);
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PlacementDriveResponse toResponse(PlacementDrive placementDrive) {
        Company company = placementDrive.getCompany();
        return new PlacementDriveResponse(
                placementDrive.getId(),
                company.getId(),
                company.getCompanyName(),
                company.getLogoUrl(),
                company.getWebsiteUrl(),
                company.getCompanyType(),
                company.getIndustry(),
                placementDrive.getDriveTitle(),
                placementDrive.getHiringYear(),
                placementDrive.getHiringDate(),
                placementDrive.getHiringMode(),
                placementDrive.getHiringLocation(),
                placementDrive.getEligibleBranches(),
                placementDrive.getEligibleCgpa(),
                placementDrive.getBacklogsAllowed(),
                placementDrive.getMaxBacklogs(),
                placementDrive.getBondDetails(),
                placementDrive.getJobType(),
                placementDrive.getCtcPackage(),
                placementDrive.getStipend(),
                placementDrive.getNumberOfRounds(),
                placementDrive.getRoundNames(),
                placementDrive.getRegistrationDeadline(),
                placementDrive.getExamDate(),
                placementDrive.getInterviewDate(),
                placementDrive.getDriveStatus(),
                placementDrive.getDescription(),
                placementDrive.getActive(),
                placementDrive.getCreatedAt(),
                placementDrive.getUpdatedAt()
        );
    }
}
