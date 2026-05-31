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
                      and (
                        :search = ''
                        or lower(c.companyName) like lower(concat('%', :search, '%'))
                        or lower(c.companyType) like lower(concat('%', :search, '%'))
                        or lower(c.industry) like lower(concat('%', :search, '%'))
                        or lower(coalesce(c.headquarters, '')) like lower(concat('%', :search, '%'))
                        or cast(coalesce(c.foundedYear, 0) as string) like concat('%', :search, '%')
                        or lower(cast(c.description as string)) like lower(concat('%', :search, '%'))
                      )
                    order by c.createdAt desc, c.id desc
                    """,
            countQuery = """
                    select count(c)
                    from Company c
                    where (:adminView = true or c.active = true)
                      and (
                        :search = ''
                        or lower(c.companyName) like lower(concat('%', :search, '%'))
                        or lower(c.companyType) like lower(concat('%', :search, '%'))
                        or lower(c.industry) like lower(concat('%', :search, '%'))
                        or lower(coalesce(c.headquarters, '')) like lower(concat('%', :search, '%'))
                        or cast(coalesce(c.foundedYear, 0) as string) like concat('%', :search, '%')
                        or lower(cast(c.description as string)) like lower(concat('%', :search, '%'))
                      )
                    """
    )
    Page<CompanyListDto> findCompanyListPage(@Param("adminView") boolean adminView, @Param("search") String search, Pageable pageable);

    boolean existsByCompanyNameIgnoreCase(String companyName);

    Optional<Company> findByCompanyNameIgnoreCase(String companyName);
}
