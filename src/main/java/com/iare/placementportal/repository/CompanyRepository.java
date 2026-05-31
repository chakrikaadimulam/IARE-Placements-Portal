package com.iare.placementportal.repository;

import com.iare.placementportal.dto.CompanyListDto;
import com.iare.placementportal.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findAllByOrderByCreatedAtDesc();

    List<Company> findByActiveTrueOrderByCreatedAtDesc();

    @Query(
            value = """
                    select new com.iare.placementportal.dto.CompanyListDto(
                        c.id,
                        c.companyName,
                        c.logoUrl,
                        c.websiteUrl,
                        c.companyType,
                        c.industry,
                        c.headquarters,
                        c.foundedYear,
                        c.description,
                        c.active,
                        c.createdAt
                    )
                    from Company c
                    where (:adminView = true or c.active = true)
                    order by c.createdAt desc, c.id desc
                    """,
            countQuery = """
                    select count(c)
                    from Company c
                    where (:adminView = true or c.active = true)
                    """
    )
    Page<CompanyListDto> findCompanyListPage(@Param("adminView") boolean adminView, Pageable pageable);

    boolean existsByCompanyNameIgnoreCase(String companyName);

    Optional<Company> findByCompanyNameIgnoreCase(String companyName);
}
