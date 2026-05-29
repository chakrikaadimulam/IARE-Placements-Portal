package com.iare.placementportal.repository;

import com.iare.placementportal.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByRollNoIgnoreCase(String rollNo);

    boolean existsByRollNoIgnoreCase(String rollNo);

    List<Student> findAllByOrderByCreatedAtDesc();

    List<Student> findByActiveTrueOrderByStudentNameAsc();

    List<Student> findByBranchIgnoreCaseAndActiveTrueOrderByStudentNameAsc(String branch);

    List<Student> findBySemesterAndActiveTrueOrderByStudentNameAsc(Integer semester);

    List<Student> findBySectionIgnoreCaseAndActiveTrueOrderByStudentNameAsc(String section);
}
