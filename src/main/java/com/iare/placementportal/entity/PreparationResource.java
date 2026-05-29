package com.iare.placementportal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "preparation_resources")
public class PreparationResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placement_drive_id", nullable = false)
    private PlacementDrive placementDrive;

    @Column(nullable = false)
    private String resourceTitle;

    @Lob
    private String description;

    @Column(length = 1000)
    private String aptitudePdfUrl;

    private String aptitudePdfPublicId;

    @Column(length = 1000)
    private String codingPdfUrl;

    private String codingPdfPublicId;

    @Column(length = 1000)
    private String technicalPdfUrl;

    private String technicalPdfPublicId;

    @Column(length = 1000)
    private String hrPdfUrl;

    private String hrPdfPublicId;

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

    public String getResourceTitle() {
        return resourceTitle;
    }

    public void setResourceTitle(String resourceTitle) {
        this.resourceTitle = resourceTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAptitudePdfUrl() {
        return aptitudePdfUrl;
    }

    public void setAptitudePdfUrl(String aptitudePdfUrl) {
        this.aptitudePdfUrl = aptitudePdfUrl;
    }

    public String getAptitudePdfPublicId() {
        return aptitudePdfPublicId;
    }

    public void setAptitudePdfPublicId(String aptitudePdfPublicId) {
        this.aptitudePdfPublicId = aptitudePdfPublicId;
    }

    public String getCodingPdfUrl() {
        return codingPdfUrl;
    }

    public void setCodingPdfUrl(String codingPdfUrl) {
        this.codingPdfUrl = codingPdfUrl;
    }

    public String getCodingPdfPublicId() {
        return codingPdfPublicId;
    }

    public void setCodingPdfPublicId(String codingPdfPublicId) {
        this.codingPdfPublicId = codingPdfPublicId;
    }

    public String getTechnicalPdfUrl() {
        return technicalPdfUrl;
    }

    public void setTechnicalPdfUrl(String technicalPdfUrl) {
        this.technicalPdfUrl = technicalPdfUrl;
    }

    public String getTechnicalPdfPublicId() {
        return technicalPdfPublicId;
    }

    public void setTechnicalPdfPublicId(String technicalPdfPublicId) {
        this.technicalPdfPublicId = technicalPdfPublicId;
    }

    public String getHrPdfUrl() {
        return hrPdfUrl;
    }

    public void setHrPdfUrl(String hrPdfUrl) {
        this.hrPdfUrl = hrPdfUrl;
    }

    public String getHrPdfPublicId() {
        return hrPdfPublicId;
    }

    public void setHrPdfPublicId(String hrPdfPublicId) {
        this.hrPdfPublicId = hrPdfPublicId;
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
