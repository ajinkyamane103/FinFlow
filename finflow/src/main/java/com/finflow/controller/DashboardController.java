package com.finflow.controller;

import com.finflow.dto.DashboardResponse;
import com.finflow.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    public DashboardController(DashboardService dashboardService) { this.dashboardService = dashboardService; }

    @GetMapping
    public DashboardResponse dashboard(Authentication authentication) {
        return dashboardService.getDashboard(authentication.getName());
    }
}
