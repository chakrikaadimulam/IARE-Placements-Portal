package com.iare.placementportal.repository;

import com.iare.placementportal.entity.SelectedStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SelectedStudentRepository extends JpaRepository<SelectedStudent, Long> {

    List<SelectedStudent> findAllByOrderByCreatedAtDesc();

    List<SelectedStudent> findByActiveTrueOrderByCreatedAtDesc();

    List<SelectedStudent> findByPlacementDriveIdOrderByCreatedAtDesc(Long placementDriveId);

    List<SelectedStudent> findByPlacementDriveIdAndActiveTrueOrderByCreatedAtDesc(Long placementDriveId);

    boolean existsByPlacementDriveIdAndRollNumberIgnoreCase(Long placementDriveId, String rollNumber);
}
