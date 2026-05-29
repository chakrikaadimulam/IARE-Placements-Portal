package com.iare.placementportal.repository;

import com.iare.placementportal.entity.PreparationResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreparationResourceRepository extends JpaRepository<PreparationResource, Long> {

    List<PreparationResource> findAllByOrderByCreatedAtDesc();

    List<PreparationResource> findByActiveTrueOrderByCreatedAtDesc();

    List<PreparationResource> findByPlacementDriveIdOrderByCreatedAtDesc(Long placementDriveId);

    List<PreparationResource> findByPlacementDriveIdAndActiveTrueOrderByCreatedAtDesc(Long placementDriveId);
}
