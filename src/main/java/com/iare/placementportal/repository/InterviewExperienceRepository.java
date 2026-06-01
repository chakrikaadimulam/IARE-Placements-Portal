package com.iare.placementportal.repository;

import com.iare.placementportal.entity.InterviewExperience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterviewExperienceRepository extends JpaRepository<InterviewExperience, Long> {

    List<InterviewExperience> findAllByOrderByCreatedAtDesc();

    List<InterviewExperience> findByActiveTrueOrderByCreatedAtDesc();

    List<InterviewExperience> findByPlacementDriveIdOrderByCreatedAtDesc(Long placementDriveId);

    List<InterviewExperience> findByPlacementDriveIdAndActiveTrueOrderByCreatedAtDesc(Long placementDriveId);

    @EntityGraph(attributePaths = {"placementDrive", "placementDrive.company"})
    @Query("""
            select ie from InterviewExperience ie
            join ie.placementDrive pd
            join pd.company c
            where ie.active = true
              and (:search = '' or
                   lower(ie.studentName) like lower(concat('%', :search, '%')) or
                   lower(c.companyName) like lower(concat('%', :search, '%')) or
                   lower(pd.driveTitle) like lower(concat('%', :search, '%')) or
                   lower(ie.roleOffered) like lower(concat('%', :search, '%')) or
                   lower(coalesce(ie.technicalTopics, '')) like lower(concat('%', :search, '%')) or
                   lower(ie.finalResult) like lower(concat('%', :search, '%')) or
                   lower(ie.difficultyLevel) like lower(concat('%', :search, '%')))
              and (:placementDriveId is null or pd.id = :placementDriveId)
              and (:difficultyLevel = '' or lower(ie.difficultyLevel) = lower(:difficultyLevel))
              and (:finalResult = '' or lower(ie.finalResult) = lower(:finalResult))
              and (:hiringYear is null or pd.hiringYear = :hiringYear)
            """)
    Page<InterviewExperience> findActivePageForStudents(@Param("search") String search,
                                                        @Param("placementDriveId") Long placementDriveId,
                                                        @Param("difficultyLevel") String difficultyLevel,
                                                        @Param("finalResult") String finalResult,
                                                        @Param("hiringYear") Integer hiringYear,
                                                        Pageable pageable);

    @Query("""
            select distinct ie.finalResult from InterviewExperience ie
            where ie.active = true
              and ie.finalResult is not null
              and trim(ie.finalResult) <> ''
            order by ie.finalResult asc
            """)
    List<String> findDistinctActiveFinalResults();

    @Query("""
            select distinct pd.hiringYear from InterviewExperience ie
            join ie.placementDrive pd
            where ie.active = true
              and pd.hiringYear is not null
            order by pd.hiringYear desc
            """)
    List<Integer> findDistinctActiveHiringYears();

    @Query("""
            select distinct pd.id, c.companyName, pd.driveTitle, pd.hiringYear
            from InterviewExperience ie
            join ie.placementDrive pd
            join pd.company c
            where ie.active = true
            order by pd.hiringYear desc, c.companyName asc, pd.driveTitle asc
            """)
    List<Object[]> findDistinctActiveDriveOptions();
}
