package com.iare.placementportal.service;

import com.iare.placementportal.dto.InterviewExperienceRequest;
import com.iare.placementportal.dto.InterviewExperienceResponse;
import com.iare.placementportal.entity.Company;
import com.iare.placementportal.entity.InterviewExperience;
import com.iare.placementportal.entity.PlacementDrive;
import com.iare.placementportal.repository.InterviewExperienceRepository;
import com.iare.placementportal.repository.PlacementDriveRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class InterviewExperienceService {

    private static final Set<String> ALLOWED_DIFFICULTY_LEVELS = Set.of("Easy", "Medium", "Hard");
    private static final Set<String> ALLOWED_FINAL_RESULTS = Set.of("Selected", "Rejected", "Waiting", "Internship Offered");

    private final InterviewExperienceRepository interviewExperienceRepository;
    private final PlacementDriveRepository placementDriveRepository;

    public InterviewExperienceService(InterviewExperienceRepository interviewExperienceRepository,
                                      PlacementDriveRepository placementDriveRepository) {
        this.interviewExperienceRepository = interviewExperienceRepository;
        this.placementDriveRepository = placementDriveRepository;
    }

    public InterviewExperienceResponse createExperience(InterviewExperienceRequest request) {
        validateRequest(request);
        PlacementDrive placementDrive = findDriveOrThrow(request.placementDriveId());

        InterviewExperience interviewExperience = new InterviewExperience();
        mapRequestToEntity(request, interviewExperience, placementDrive);

        return toResponse(interviewExperienceRepository.save(interviewExperience));
    }

    @Transactional(readOnly = true)
    public List<InterviewExperienceResponse> getAllExperiences() {
        return interviewExperienceRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewExperienceResponse> getActiveExperiencesForStudents() {
        return interviewExperienceRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewExperienceResponse> getExperiencesByDrive(Long placementDriveId) {
        findDriveOrThrow(placementDriveId);
        return interviewExperienceRepository.findByPlacementDriveIdOrderByCreatedAtDesc(placementDriveId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewExperienceResponse> getActiveExperiencesByDrive(Long placementDriveId) {
        findDriveOrThrow(placementDriveId);
        return interviewExperienceRepository.findByPlacementDriveIdAndActiveTrueOrderByCreatedAtDesc(placementDriveId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public InterviewExperienceResponse updateExperience(Long id, InterviewExperienceRequest request) {
        validateRequest(request);

        InterviewExperience interviewExperience = findExperienceOrThrow(id);
        PlacementDrive placementDrive = findDriveOrThrow(request.placementDriveId());
        mapRequestToEntity(request, interviewExperience, placementDrive);

        return toResponse(interviewExperienceRepository.save(interviewExperience));
    }

    public void deleteExperience(Long id) {
        InterviewExperience interviewExperience = findExperienceOrThrow(id);
        interviewExperienceRepository.delete(interviewExperience);
    }

    public InterviewExperienceResponse changeExperienceActiveStatus(Long id, boolean active) {
        InterviewExperience interviewExperience = findExperienceOrThrow(id);
        interviewExperience.setActive(active);
        return toResponse(interviewExperienceRepository.save(interviewExperience));
    }

    private PlacementDrive findDriveOrThrow(Long placementDriveId) {
        return placementDriveRepository.findById(placementDriveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected placement drive does not exist."));
    }

    private InterviewExperience findExperienceOrThrow(Long id) {
        return interviewExperienceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview experience record not found."));
    }

    private void validateRequest(InterviewExperienceRequest request) {
        validateUrl(request.studentPhotoUrl(), "Student Photo URL must be a valid URL.");

        if (!ALLOWED_DIFFICULTY_LEVELS.contains(request.difficultyLevel().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Difficulty Level must be Easy, Medium, or Hard.");
        }

        if (!ALLOWED_FINAL_RESULTS.contains(request.finalResult().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Final Result must be Selected, Rejected, Waiting, or Internship Offered.");
        }
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

    private void mapRequestToEntity(InterviewExperienceRequest request,
                                    InterviewExperience interviewExperience,
                                    PlacementDrive placementDrive) {
        interviewExperience.setPlacementDrive(placementDrive);
        interviewExperience.setStudentName(request.studentName().trim());
        interviewExperience.setStudentPhotoUrl(normalizeOptional(request.studentPhotoUrl()));
        interviewExperience.setRoleOffered(request.roleOffered().trim());
        interviewExperience.setDifficultyLevel(request.difficultyLevel().trim());
        interviewExperience.setRoundsFaced(request.roundsFaced().trim());
        interviewExperience.setQuestionsAsked(request.questionsAsked().trim());
        interviewExperience.setCodingQuestions(normalizeOptional(request.codingQuestions()));
        interviewExperience.setTechnicalTopics(normalizeOptional(request.technicalTopics()));
        interviewExperience.setHrQuestions(normalizeOptional(request.hrQuestions()));
        interviewExperience.setPreparationTips(request.preparationTips().trim());
        interviewExperience.setFinalResult(request.finalResult().trim());
        interviewExperience.setExperienceDate(request.experienceDate());
        if (interviewExperience.getActive() == null) {
            interviewExperience.setActive(true);
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private InterviewExperienceResponse toResponse(InterviewExperience interviewExperience) {
        PlacementDrive placementDrive = interviewExperience.getPlacementDrive();
        Company company = placementDrive.getCompany();

        return new InterviewExperienceResponse(
                interviewExperience.getId(),
                placementDrive.getId(),
                placementDrive.getDriveTitle(),
                placementDrive.getHiringYear(),
                placementDrive.getHiringDate(),
                company.getId(),
                company.getCompanyName(),
                company.getLogoUrl(),
                company.getWebsiteUrl(),
                interviewExperience.getRoleOffered(),
                interviewExperience.getStudentName(),
                interviewExperience.getStudentPhotoUrl(),
                interviewExperience.getDifficultyLevel(),
                interviewExperience.getRoundsFaced(),
                interviewExperience.getQuestionsAsked(),
                interviewExperience.getCodingQuestions(),
                interviewExperience.getTechnicalTopics(),
                interviewExperience.getHrQuestions(),
                interviewExperience.getPreparationTips(),
                interviewExperience.getFinalResult(),
                interviewExperience.getExperienceDate(),
                interviewExperience.getActive(),
                interviewExperience.getCreatedAt(),
                interviewExperience.getUpdatedAt()
        );
    }
}
