package com.iare.placementportal.repository;

import com.iare.placementportal.entity.PlacementDrive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlacementDriveRepository extends JpaRepository<PlacementDrive, Long> {

    List<PlacementDrive> findAllByOrderByCreatedAtDesc();

    List<PlacementDrive> findByActiveTrueOrderByCreatedAtDesc();

    List<PlacementDrive> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<PlacementDrive> findByCompanyIdAndActiveTrueOrderByCreatedAtDesc(Long companyId);
}
