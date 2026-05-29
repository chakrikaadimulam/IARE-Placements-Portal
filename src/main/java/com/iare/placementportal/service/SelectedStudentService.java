package com.iare.placementportal.service;

import com.iare.placementportal.dto.SelectedStudentRequest;
import com.iare.placementportal.dto.SelectedStudentResponse;
import com.iare.placementportal.entity.Company;
import com.iare.placementportal.entity.PlacementDrive;
import com.iare.placementportal.entity.SelectedStudent;
import com.iare.placementportal.repository.PlacementDriveRepository;
import com.iare.placementportal.repository.SelectedStudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Service
@Transactional
public class SelectedStudentService {

    private final SelectedStudentRepository selectedStudentRepository;
    private final PlacementDriveRepository placementDriveRepository;

    public SelectedStudentService(SelectedStudentRepository selectedStudentRepository,
                                  PlacementDriveRepository placementDriveRepository) {
        this.selectedStudentRepository = selectedStudentRepository;
        this.placementDriveRepository = placementDriveRepository;
    }

    public SelectedStudentResponse createSelectedStudent(SelectedStudentRequest request) {
        validateRequest(request);
        PlacementDrive placementDrive = findDriveOrThrow(request.placementDriveId());

        if (selectedStudentRepository.existsByPlacementDriveIdAndRollNumberIgnoreCase(
                request.placementDriveId(), request.rollNumber().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This roll number already exists for the selected placement drive.");
        }

        SelectedStudent selectedStudent = new SelectedStudent();
        mapRequestToEntity(request, selectedStudent, placementDrive);

        return toResponse(selectedStudentRepository.save(selectedStudent));
    }

    @Transactional(readOnly = true)
    public List<SelectedStudentResponse> getAllSelectedStudents() {
        return selectedStudentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SelectedStudentResponse> getActiveSelectedStudentsForStudents() {
        return selectedStudentRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SelectedStudentResponse> getSelectedStudentsByDrive(Long placementDriveId) {
        findDriveOrThrow(placementDriveId);
        return selectedStudentRepository.findByPlacementDriveIdOrderByCreatedAtDesc(placementDriveId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SelectedStudentResponse> getActiveSelectedStudentsByDrive(Long placementDriveId) {
        findDriveOrThrow(placementDriveId);
        return selectedStudentRepository.findByPlacementDriveIdAndActiveTrueOrderByCreatedAtDesc(placementDriveId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SelectedStudentResponse updateSelectedStudent(Long id, SelectedStudentRequest request) {
        validateRequest(request);

        SelectedStudent selectedStudent = findSelectedStudentOrThrow(id);
        PlacementDrive placementDrive = findDriveOrThrow(request.placementDriveId());

        List<SelectedStudent> studentsInDrive = selectedStudentRepository.findByPlacementDriveIdOrderByCreatedAtDesc(request.placementDriveId());
        boolean duplicateExists = studentsInDrive.stream()
                .anyMatch(existing -> !existing.getId().equals(id)
                        && existing.getRollNumber().equalsIgnoreCase(request.rollNumber().trim()));

        if (duplicateExists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This roll number already exists for the selected placement drive.");
        }

        mapRequestToEntity(request, selectedStudent, placementDrive);
        return toResponse(selectedStudentRepository.save(selectedStudent));
    }

    public void deleteSelectedStudent(Long id) {
        SelectedStudent selectedStudent = findSelectedStudentOrThrow(id);
        selectedStudentRepository.delete(selectedStudent);
    }

    public SelectedStudentResponse changeSelectedStudentActiveStatus(Long id, boolean active) {
        SelectedStudent selectedStudent = findSelectedStudentOrThrow(id);
        selectedStudent.setActive(active);
        return toResponse(selectedStudentRepository.save(selectedStudent));
    }

    private PlacementDrive findDriveOrThrow(Long placementDriveId) {
        return placementDriveRepository.findById(placementDriveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected placement drive does not exist."));
    }

    private SelectedStudent findSelectedStudentOrThrow(Long id) {
        return selectedStudentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected student record not found."));
    }

    private void validateRequest(SelectedStudentRequest request) {
        validateUrl(request.photoUrl(), "Photo URL must be a valid URL.");
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

    private void mapRequestToEntity(SelectedStudentRequest request,
                                    SelectedStudent selectedStudent,
                                    PlacementDrive placementDrive) {
        selectedStudent.setPlacementDrive(placementDrive);
        selectedStudent.setStudentName(request.studentName().trim());
        selectedStudent.setRollNumber(request.rollNumber().trim());
        selectedStudent.setBranch(request.branch().trim());
        selectedStudent.setSection(request.section().trim());
        selectedStudent.setGender(request.gender().trim());
        selectedStudent.setPhotoUrl(normalizeOptional(request.photoUrl()));
        selectedStudent.setPackageOffered(request.packageOffered().trim());
        selectedStudent.setRoleOffered(request.roleOffered().trim());
        selectedStudent.setOfferType(request.offerType().trim());
        selectedStudent.setSelectionDate(request.selectionDate());
        if (selectedStudent.getActive() == null) {
            selectedStudent.setActive(true);
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SelectedStudentResponse toResponse(SelectedStudent selectedStudent) {
        PlacementDrive placementDrive = selectedStudent.getPlacementDrive();
        Company company = placementDrive.getCompany();

        return new SelectedStudentResponse(
                selectedStudent.getId(),
                placementDrive.getId(),
                placementDrive.getDriveTitle(),
                placementDrive.getHiringYear(),
                company.getId(),
                company.getCompanyName(),
                company.getLogoUrl(),
                selectedStudent.getStudentName(),
                selectedStudent.getRollNumber(),
                selectedStudent.getBranch(),
                selectedStudent.getSection(),
                selectedStudent.getGender(),
                selectedStudent.getPhotoUrl(),
                selectedStudent.getPackageOffered(),
                selectedStudent.getRoleOffered(),
                selectedStudent.getOfferType(),
                selectedStudent.getSelectionDate(),
                selectedStudent.getActive(),
                selectedStudent.getCreatedAt(),
                selectedStudent.getUpdatedAt()
        );
    }
}
