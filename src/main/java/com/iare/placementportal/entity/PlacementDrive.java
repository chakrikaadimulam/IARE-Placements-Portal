package com.iare.placementportal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "placement_drives")
public class PlacementDrive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String driveTitle;

    @Column(nullable = false)
    private Integer hiringYear;

    @Column(nullable = false)
    private LocalDate hiringDate;

    @Column(nullable = false)
    private String hiringMode;

    private String hiringLocation;

    @Column(nullable = false)
    private String eligibleBranches;

    @Column(nullable = false)
    private Double eligibleCgpa;

    @Column(nullable = false)
    private Boolean backlogsAllowed = false;

    private Integer maxBacklogs;

    @Column(columnDefinition = "TEXT")
    private String bondDetails;

    @Column(nullable = false)
    private String jobType;

    @Column(nullable = false)
    private String ctcPackage;

    private String stipend;

    private Integer numberOfRounds;

    @Column(columnDefinition = "TEXT")
    private String roundNames;

    private LocalDate registrationDeadline;

    private LocalDate examDate;

    private LocalDate interviewDate;

    @Column(nullable = false)
    private String driveStatus;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean active = true;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.active == null) {
            this.active = true;
        }
        if (this.backlogsAllowed == null) {
            this.backlogsAllowed = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getDriveTitle() {
        return driveTitle;
    }

    public void setDriveTitle(String driveTitle) {
        this.driveTitle = driveTitle;
    }

    public Integer getHiringYear() {
        return hiringYear;
    }

    public void setHiringYear(Integer hiringYear) {
        this.hiringYear = hiringYear;
    }

    public LocalDate getHiringDate() {
        return hiringDate;
    }

    public void setHiringDate(LocalDate hiringDate) {
        this.hiringDate = hiringDate;
    }

    public String getHiringMode() {
        return hiringMode;
    }

    public void setHiringMode(String hiringMode) {
        this.hiringMode = hiringMode;
    }

    public String getHiringLocation() {
        return hiringLocation;
    }

    public void setHiringLocation(String hiringLocation) {
        this.hiringLocation = hiringLocation;
    }

    public String getEligibleBranches() {
        return eligibleBranches;
    }

    public void setEligibleBranches(String eligibleBranches) {
        this.eligibleBranches = eligibleBranches;
    }

    public Double getEligibleCgpa() {
        return eligibleCgpa;
    }

    public void setEligibleCgpa(Double eligibleCgpa) {
        this.eligibleCgpa = eligibleCgpa;
    }

    public Boolean getBacklogsAllowed() {
        return backlogsAllowed;
    }

    public void setBacklogsAllowed(Boolean backlogsAllowed) {
        this.backlogsAllowed = backlogsAllowed;
    }

    public Integer getMaxBacklogs() {
        return maxBacklogs;
    }

    public void setMaxBacklogs(Integer maxBacklogs) {
        this.maxBacklogs = maxBacklogs;
    }

    public String getBondDetails() {
        return bondDetails;
    }

    public void setBondDetails(String bondDetails) {
        this.bondDetails = bondDetails;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getCtcPackage() {
        return ctcPackage;
    }

    public void setCtcPackage(String ctcPackage) {
        this.ctcPackage = ctcPackage;
    }

    public String getStipend() {
        return stipend;
    }

    public void setStipend(String stipend) {
        this.stipend = stipend;
    }

    public Integer getNumberOfRounds() {
        return numberOfRounds;
    }

    public void setNumberOfRounds(Integer numberOfRounds) {
        this.numberOfRounds = numberOfRounds;
    }

    public String getRoundNames() {
        return roundNames;
    }

    public void setRoundNames(String roundNames) {
        this.roundNames = roundNames;
    }

    public LocalDate getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(LocalDate registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public LocalDate getInterviewDate() {
        return interviewDate;
    }

    public void setInterviewDate(LocalDate interviewDate) {
        this.interviewDate = interviewDate;
    }

    public String getDriveStatus() {
        return driveStatus;
    }

    public void setDriveStatus(String driveStatus) {
        this.driveStatus = driveStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
