package com.iare.placementportal.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/",
                                "/admin",
                                "/student",
                                "/admin-dashboard",
                                "/admin-notices",
                                "/admin-companies",
                                "/admin-placement-drives",
                                "/admin-placement-statistics",
                                "/admin-selected-students",
                                "/admin-interview-experiences",
                                "/student-dashboard",
                                "/student-notices",
                                "/student-companies",
                                "/student-placement-drives",
                                "/student-placement-statistics",
                                "/student-selected-students",
                                "/student-interview-experiences",
                                "/api/admin/notices/**",
                                "/api/student/notices/**",
                                "/api/admin/companies/**",
                                "/api/student/companies/**",
                                "/api/admin/placement-drives/**",
                                "/api/student/placement-drives/**",
                                "/api/admin/placement-statistics/**",
                                "/api/student/placement-statistics/**",
                                "/api/admin/selected-students/**",
                                "/api/student/selected-students/**",
                                "/api/admin/interview-experiences/**",
                                "/api/student/interview-experiences/**",
                                "/**"
                        ).permitAll()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
