package com.smartflow.controller;

import com.smartflow.dto.DashboardDtos;
import com.smartflow.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tableaux de bord statistiques selon le rôle.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public DashboardDtos.AdminStats admin() {
        return dashboardService.adminStats();
    }

    @GetMapping("/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public DashboardDtos.ManagerStats manager() {
        return dashboardService.managerStats();
    }

    @GetMapping("/technician")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public DashboardDtos.TechnicianStats technician() {
        return dashboardService.technicianStats();
    }
}