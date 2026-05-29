package com.iare.placementportal.service;

import com.iare.placementportal.dto.LoginRequest;
import com.iare.placementportal.entity.UserRole;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String STUDENT_USERNAME = "student";
    private static final String STUDENT_PASSWORD = "student123";

    public boolean isValid(LoginRequest loginRequest) {
        if (loginRequest == null || loginRequest.role() == null) {
            return false;
        }

        return switch (UserRole.valueOf(loginRequest.role().toUpperCase())) {
            case ADMIN -> ADMIN_USERNAME.equals(loginRequest.username())
                    && ADMIN_PASSWORD.equals(loginRequest.password());
            case STUDENT -> STUDENT_USERNAME.equals(loginRequest.username())
                    && STUDENT_PASSWORD.equals(loginRequest.password());
        };
    }
}
