package com.iare.placementportal.repository;

import com.iare.placementportal.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findAllByOrderByCreatedAtDesc();

    List<Company> findByActiveTrueOrderByCreatedAtDesc();

    boolean existsByCompanyNameIgnoreCase(String companyName);

    Optional<Company> findByCompanyNameIgnoreCase(String companyName);
}
