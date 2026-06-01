package com.iare.placementportal.service;

import com.iare.placementportal.dto.PlacementStatisticsRequest;
import com.iare.placementportal.dto.PlacementStatisticsResponse;
import com.iare.placementportal.dto.PagedResponse;
import com.iare.placementportal.dto.PlacementStatisticsFilterOptionsResponse;
import com.iare.placementportal.entity.Company;
import com.iare.placementportal.entity.PlacementDrive;
import com.iare.placementportal.entity.PlacementStatistics;
import com.iare.placementportal.repository.PlacementDriveRepository;
import com.iare.placementportal.repository.PlacementStatisticsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class PlacementStatisticsService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final PlacementStatisticsRepository placementStatisticsRepository;
    private final PlacementDriveRepository placementDriveRepository;

    public PlacementStatisticsService(PlacementStatisticsRepository placementStatisticsRepository,
                                      PlacementDriveRepository placementDriveRepository) {
        this.placementStatisticsRepository = placementStatisticsRepository;
        this.placementDriveRepository = placementDriveRepository;
    }

    public PlacementStatisticsResponse createStatistics(PlacementStatisticsRequest request) {
        validateRequest(request);
        PlacementDrive placementDrive = findDriveOrThrow(request.placementDriveId());

        if (placementStatisticsRepository.existsByPlacementDriveId(request.placementDriveId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This placement drive already has a statistics record.");
        }

        PlacementStatistics placementStatistics = new PlacementStatistics();
        mapRequestToEntity(request, placementStatistics, placementDrive);

        return toResponse(placementStatisticsRepository.save(placementStatistics));
    }

    @Transactional(readOnly = true)
    public List<PlacementStatisticsResponse> getAllStatistics() {
        return placementStatisticsRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlacementStatisticsResponse> getActiveStatisticsForStudents() {
        return placementStatisticsRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<PlacementStatisticsResponse> getActiveStatisticsPageForStudents(int page,
                                                                                         int size,
                                                                                         String search,
                                                                                         Integer hiringYear,
                                                                                         String driveStatus) {
        Page<PlacementStatisticsResponse> resultPage = placementStatisticsRepository.findActivePageForStudents(
                        normalizeFilter(search),
                        hiringYear,
                        normalizeFilter(driveStatus),
                        PageRequest.of(
                                sanitizePage(page),
                                sanitizeSize(size),
                                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
                        )
                )
                .map(this::toResponse);

        return PagedResponse.from(resultPage);
    }

    @Transactional(readOnly = true)
    public PlacementStatisticsFilterOptionsResponse getActiveStatisticsFilterOptions() {
        return new PlacementStatisticsFilterOptionsResponse(
                placementStatisticsRepository.findDistinctActiveHiringYears()
        );
    }

    @Transactional(readOnly = true)
    public PlacementStatisticsResponse getStatisticsByDrive(Long placementDriveId) {
        findDriveOrThrow(placementDriveId);
        return placementStatisticsRepository.findByPlacementDriveId(placementDriveId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statistics not found for this placement drive."));
    }

    public PlacementStatisticsResponse updateStatistics(Long id, PlacementStatisticsRequest request) {
        validateRequest(request);

        PlacementStatistics placementStatistics = findStatisticsOrThrow(id);
        PlacementDrive placementDrive = findDriveOrThrow(request.placementDriveId());

        placementStatisticsRepository.findByPlacementDriveId(request.placementDriveId())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This placement drive already has a statistics record.");
                });

        mapRequestToEntity(request, placementStatistics, placementDrive);

        return toResponse(placementStatisticsRepository.save(placementStatistics));
    }

    public void deleteStatistics(Long id) {
        PlacementStatistics placementStatistics = findStatisticsOrThrow(id);
        placementStatisticsRepository.delete(placementStatistics);
    }

    public PlacementStatisticsResponse changeStatisticsActiveStatus(Long id, boolean active) {
        PlacementStatistics placementStatistics = findStatisticsOrThrow(id);
        placementStatistics.setActive(active);
        return toResponse(placementStatisticsRepository.save(placementStatistics));
    }

    private PlacementDrive findDriveOrThrow(Long placementDriveId) {
        return placementDriveRepository.findById(placementDriveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected placement drive does not exist."));
    }

    private PlacementStatistics findStatisticsOrThrow(Long id) {
        return placementStatisticsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Placement statistics record not found."));
    }

    private void validateRequest(PlacementStatisticsRequest request) {
        int studentsApplied = normalizedCount(request.studentsApplied());
        int studentsAttended = normalizedCount(request.studentsAttended());
        int studentsShortlisted = normalizedCount(request.studentsShortlisted());
        int studentsSelected = normalizedCount(request.studentsSelected());
        int maleSelected = normalizedCount(request.maleSelected());
        int femaleSelected = normalizedCount(request.femaleSelected());

        validateNonNegative(studentsApplied, "Students Applied must be 0 or positive.");
        validateNonNegative(studentsAttended, "Students Attended must be 0 or positive.");
        validateNonNegative(studentsShortlisted, "Students Shortlisted must be 0 or positive.");
        validateNonNegative(studentsSelected, "Students Selected must be 0 or positive.");
        validateNonNegative(maleSelected, "Male Selected must be 0 or positive.");
        validateNonNegative(femaleSelected, "Female Selected must be 0 or positive.");

        if (studentsAttended > studentsApplied) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Students Attended cannot be greater than Students Applied.");
        }
        if (studentsShortlisted > studentsAttended) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Students Shortlisted cannot be greater than Students Attended.");
        }
        if (studentsSelected > studentsShortlisted) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Students Selected cannot be greater than Students Shortlisted.");
        }
        if (maleSelected + femaleSelected > studentsSelected) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Male Selected + Female Selected cannot be greater than Students Selected.");
        }

        validatePackage(request.highestPackage(), "Highest Package must be 0 or positive.");
        validatePackage(request.averagePackage(), "Average Package must be 0 or positive.");
        validatePackage(request.lowestPackage(), "Lowest Package must be 0 or positive.");

        if (request.highestPackage() != null && request.averagePackage() != null && request.highestPackage() < request.averagePackage()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Highest Package must be greater than or equal to Average Package.");
        }
        if (request.averagePackage() != null && request.lowestPackage() != null && request.averagePackage() < request.lowestPackage()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Average Package must be greater than or equal to Lowest Package.");
        }
        if (request.highestPackage() != null && request.lowestPackage() != null && request.highestPackage() < request.lowestPackage()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Highest Package must be greater than or equal to Lowest Package.");
        }
    }

    private void mapRequestToEntity(PlacementStatisticsRequest request,
                                    PlacementStatistics placementStatistics,
                                    PlacementDrive placementDrive) {
        placementStatistics.setPlacementDrive(placementDrive);
        placementStatistics.setStudentsApplied(normalizedCount(request.studentsApplied()));
        placementStatistics.setStudentsAttended(normalizedCount(request.studentsAttended()));
        placementStatistics.setStudentsShortlisted(normalizedCount(request.studentsShortlisted()));
        placementStatistics.setStudentsSelected(normalizedCount(request.studentsSelected()));
        placementStatistics.setMaleSelected(normalizedCount(request.maleSelected()));
        placementStatistics.setFemaleSelected(normalizedCount(request.femaleSelected()));
        placementStatistics.setHighestPackage(request.highestPackage());
        placementStatistics.setAveragePackage(request.averagePackage());
        placementStatistics.setLowestPackage(request.lowestPackage());
        if (placementStatistics.getActive() == null) {
            placementStatistics.setActive(true);
        }
    }

    private int normalizedCount(Integer value) {
        return value == null ? 0 : value;
    }

    private void validateNonNegative(int value, String message) {
        if (value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void validatePackage(Double value, String message) {
        if (value != null && value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private int sanitizePage(int page) {
        return Math.max(0, page);
    }

    private int sanitizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeFilter(String value) {
        return value == null ? "" : value.trim();
    }

    private PlacementStatisticsResponse toResponse(PlacementStatistics placementStatistics) {
        PlacementDrive placementDrive = placementStatistics.getPlacementDrive();
        Company company = placementDrive.getCompany();

        return new PlacementStatisticsResponse(
                placementStatistics.getId(),
                placementDrive.getId(),
                placementDrive.getDriveTitle(),
                placementDrive.getHiringYear(),
                placementDrive.getHiringDate(),
                placementDrive.getDriveStatus(),
                company.getId(),
                company.getCompanyName(),
                company.getLogoUrl(),
                company.getWebsiteUrl(),
                company.getCompanyType(),
                company.getIndustry(),
                placementStatistics.getStudentsApplied(),
                placementStatistics.getStudentsAttended(),
                placementStatistics.getStudentsShortlisted(),
                placementStatistics.getStudentsSelected(),
                placementStatistics.getMaleSelected(),
                placementStatistics.getFemaleSelected(),
                placementStatistics.getHighestPackage(),
                placementStatistics.getAveragePackage(),
                placementStatistics.getLowestPackage(),
                placementStatistics.getActive(),
                placementStatistics.getCreatedAt(),
                placementStatistics.getUpdatedAt()
        );
    }
}
