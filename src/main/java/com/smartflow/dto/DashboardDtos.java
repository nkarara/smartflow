package com.smartflow.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs des tableaux de bord.
 */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record AdminStats(
            long totalUsers,
            long totalClients,
            long totalTechnicians,
            long totalInterventions,
            long inProgress,
            long urgent,
            long closed,
            double resolutionRate,
            Double avgResolutionHours,
            List<MonthCount> byMonth,
            List<NameCount> byCategory,
            List<NameCount> byPriority,
            List<TechnicianPerf> technicianPerformance
    ) {
    }

    public record TechnicianStats(
            long myInterventions,
            long today,
            long inProgress,
            long completed,
            Double avgInterventionHours,
            List<MonthCount> byMonth
    ) {
    }

    public record ManagerStats(
            long totalInterventions,
            long inProgress,
            long urgent,
            long closed,
            double resolutionRate,
            List<NameCount> byPriority,
            List<TechnicianPerf> technicianPerformance
    ) {
    }

    public record MonthCount(String month, long count) {
    }

    public record NameCount(String name, long count) {
    }

    public record TechnicianPerf(
            String technicianName,
            long completed,
            long active,
            Double avgHours
    ) {
    }

    public record InterventionItem(
            Long id,
            String title,
            String status,
            String priority,
            String clientName,
            String technicianName,
            LocalDateTime createdAt
    ) {
    }
}