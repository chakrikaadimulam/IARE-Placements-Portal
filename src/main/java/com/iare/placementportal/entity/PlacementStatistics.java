package com.iare.placementportal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "placement_statistics")
public class PlacementStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placement_drive_id", nullable = false, unique = true)
    private PlacementDrive placementDrive;

    @Column(nullable = false)
    private Integer studentsApplied = 0;

    @Column(nullable = false)
    private Integer studentsAttended = 0;

    @Column(nullable = false)
    private Integer studentsShortlisted = 0;

    @Column(nullable = false)
    private Integer studentsSelected = 0;

    @Column(nullable = false)
    private Integer maleSelected = 0;

    @Column(nullable = false)
    private Integer femaleSelected = 0;

    private Double highestPackage;

    private Double averagePackage;

    private Double lowestPackage;

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
        if (this.studentsApplied == null) {
            this.studentsApplied = 0;
        }
        if (this.studentsAttended == null) {
            this.studentsAttended = 0;
        }
        if (this.studentsShortlisted == null) {
            this.studentsShortlisted = 0;
        }
        if (this.studentsSelected == null) {
            this.studentsSelected = 0;
        }
        if (this.maleSelected == null) {
            this.maleSelected = 0;
        }
        if (this.femaleSelected == null) {
            this.femaleSelected = 0;
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

    public PlacementDrive getPlacementDrive() {
        return placementDrive;
    }

    public void setPlacementDrive(PlacementDrive placementDrive) {
        this.placementDrive = placementDrive;
    }

    public Integer getStudentsApplied() {
        return studentsApplied;
    }

    public void setStudentsApplied(Integer studentsApplied) {
        this.studentsApplied = studentsApplied;
    }

    public Integer getStudentsAttended() {
        return studentsAttended;
    }

    public void setStudentsAttended(Integer studentsAttended) {
        this.studentsAttended = studentsAttended;
    }

    public Integer getStudentsShortlisted() {
        return studentsShortlisted;
    }

    public void setStudentsShortlisted(Integer studentsShortlisted) {
        this.studentsShortlisted = studentsShortlisted;
    }

    public Integer getStudentsSelected() {
        return studentsSelected;
    }

    public void setStudentsSelected(Integer studentsSelected) {
        this.studentsSelected = studentsSelected;
    }

    public Integer getMaleSelected() {
        return maleSelected;
    }

    public void setMaleSelected(Integer maleSelected) {
        this.maleSelected = maleSelected;
    }

    public Integer getFemaleSelected() {
        return femaleSelected;
    }

    public void setFemaleSelected(Integer femaleSelected) {
        this.femaleSelected = femaleSelected;
    }

    public Double getHighestPackage() {
        return highestPackage;
    }

    public void setHighestPackage(Double highestPackage) {
        this.highestPackage = highestPackage;
    }

    public Double getAveragePackage() {
        return averagePackage;
    }

    public void setAveragePackage(Double averagePackage) {
        this.averagePackage = averagePackage;
    }

    public Double getLowestPackage() {
        return lowestPackage;
    }

    public void setLowestPackage(Double lowestPackage) {
        this.lowestPackage = lowestPackage;
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
