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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_experiences")
public class InterviewExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placement_drive_id", nullable = false)
    private PlacementDrive placementDrive;

    @Column(nullable = false)
    private String studentName;

    @Column(length = 1000)
    private String studentPhotoUrl;

    @Column(nullable = false)
    private String roleOffered;

    @Column(nullable = false)
    private String difficultyLevel;

    @Column(nullable = false)
    private String roundsFaced;

    @Lob
    @Column(nullable = false)
    private String questionsAsked;

    @Lob
    private String codingQuestions;

    @Lob
    private String technicalTopics;

    @Lob
    private String hrQuestions;

    @Lob
    @Column(nullable = false)
    private String preparationTips;

    @Column(nullable = false)
    private String finalResult;

    @Column(nullable = false)
    private LocalDate experienceDate;

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

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentPhotoUrl() {
        return studentPhotoUrl;
    }

    public void setStudentPhotoUrl(String studentPhotoUrl) {
        this.studentPhotoUrl = studentPhotoUrl;
    }

    public String getRoleOffered() {
        return roleOffered;
    }

    public void setRoleOffered(String roleOffered) {
        this.roleOffered = roleOffered;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getRoundsFaced() {
        return roundsFaced;
    }

    public void setRoundsFaced(String roundsFaced) {
        this.roundsFaced = roundsFaced;
    }

    public String getQuestionsAsked() {
        return questionsAsked;
    }

    public void setQuestionsAsked(String questionsAsked) {
        this.questionsAsked = questionsAsked;
    }

    public String getCodingQuestions() {
        return codingQuestions;
    }

    public void setCodingQuestions(String codingQuestions) {
        this.codingQuestions = codingQuestions;
    }

    public String getTechnicalTopics() {
        return technicalTopics;
    }

    public void setTechnicalTopics(String technicalTopics) {
        this.technicalTopics = technicalTopics;
    }

    public String getHrQuestions() {
        return hrQuestions;
    }

    public void setHrQuestions(String hrQuestions) {
        this.hrQuestions = hrQuestions;
    }

    public String getPreparationTips() {
        return preparationTips;
    }

    public void setPreparationTips(String preparationTips) {
        this.preparationTips = preparationTips;
    }

    public String getFinalResult() {
        return finalResult;
    }

    public void setFinalResult(String finalResult) {
        this.finalResult = finalResult;
    }

    public LocalDate getExperienceDate() {
        return experienceDate;
    }

    public void setExperienceDate(LocalDate experienceDate) {
        this.experienceDate = experienceDate;
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
