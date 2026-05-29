package com.iare.placementportal.repository;

import com.iare.placementportal.entity.PlacementStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlacementStatisticsRepository extends JpaRepository<PlacementStatistics, Long> {

    List<PlacementStatistics> findAllByOrderByCreatedAtDesc();

    List<PlacementStatistics> findByActiveTrueOrderByCreatedAtDesc();

    Optional<PlacementStatistics> findByPlacementDriveId(Long placementDriveId);

    boolean existsByPlacementDriveId(Long placementDriveId);
}
