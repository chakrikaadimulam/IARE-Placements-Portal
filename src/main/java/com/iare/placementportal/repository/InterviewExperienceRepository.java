package com.iare.placementportal.repository;

import com.iare.placementportal.entity.InterviewExperience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewExperienceRepository extends JpaRepository<InterviewExperience, Long> {

    List<InterviewExperience> findAllByOrderByCreatedAtDesc();

    List<InterviewExperience> findByActiveTrueOrderByCreatedAtDesc();

    List<InterviewExperience> findByPlacementDriveIdOrderByCreatedAtDesc(Long placementDriveId);

    List<InterviewExperience> findByPlacementDriveIdAndActiveTrueOrderByCreatedAtDesc(Long placementDriveId);
}
