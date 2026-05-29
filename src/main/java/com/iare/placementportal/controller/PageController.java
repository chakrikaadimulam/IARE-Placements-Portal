package com.iare.placementportal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    @GetMapping("/admin")
    public String adminLogin() {
        return "forward:/admin-login.html";
    }

    @GetMapping("/student")
    public String studentLogin() {
        return "forward:/student-login.html";
    }

    @GetMapping("/admin-dashboard")
    public String adminDashboard() {
        return "forward:/admin-dashboard.html";
    }

    @GetMapping("/admin-notices")
    public String adminNotices() {
        return "forward:/admin-notices.html";
    }

    @GetMapping("/admin-companies")
    public String adminCompanies() {
        return "forward:/admin-companies.html";
    }

    @GetMapping("/admin-placement-drives")
    public String adminPlacementDrives() {
        return "forward:/admin-placement-drives.html";
    }

    @GetMapping("/admin-placement-statistics")
    public String adminPlacementStatistics() {
        return "forward:/admin-placement-statistics.html";
    }

    @GetMapping("/admin-selected-students")
    public String adminSelectedStudents() {
        return "forward:/admin-selected-students.html";
    }

    @GetMapping("/admin-interview-experiences")
    public String adminInterviewExperiences() {
        return "forward:/admin-interview-experiences.html";
    }

    @GetMapping("/student-dashboard")
    public String studentDashboard() {
        return "forward:/student-dashboard.html";
    }

    @GetMapping("/student-notices")
    public String studentNotices() {
        return "forward:/student-notices.html";
    }

    @GetMapping("/student-companies")
    public String studentCompanies() {
        return "forward:/student-companies.html";
    }

    @GetMapping("/student-placement-drives")
    public String studentPlacementDrives() {
        return "forward:/student-placement-drives.html";
    }

    @GetMapping("/student-placement-statistics")
    public String studentPlacementStatistics() {
        return "forward:/student-placement-statistics.html";
    }

    @GetMapping("/student-selected-students")
    public String studentSelectedStudents() {
        return "forward:/student-selected-students.html";
    }

    @GetMapping("/student-interview-experiences")
    public String studentInterviewExperiences() {
        return "forward:/student-interview-experiences.html";
    }
}
