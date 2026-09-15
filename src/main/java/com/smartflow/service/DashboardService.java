package com.smartflow.service;

import com.smartflow.dto.DashboardDtos;
import com.smartflow.entity.Intervention;
import com.smartflow.entity.Priority;
import com.smartflow.entity.Status;
import com.smartflow.entity.Technician;
import com.smartflow.entity.User;
import com.smartflow.repository.ClientRepository;
import com.smartflow.repository.InterventionRepository;
import com.smartflow.repository.TechnicianRepository;
import com.smartflow.repository.UserRepository;
import com.smartflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Statistiques et tableaux de bord selon le rôle.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InterventionRepository interventionRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TechnicianRepository technicianRepository;
    private final TechnicianService technicianService;

    private static final List<Priority> URGENT_PRIORITIES = List.of(Priority.HIGH, Priority.URGENT);
    private static final List<Status> FINISHED = List.of(Status.RESOLVED, Status.CLOSED);

    @Transactional(readOnly = true)
    public DashboardDtos.AdminStats adminStats() {
        long total = interventionRepository.count();
        long closed = interventionRepository.countByStatus(Status.CLOSED);
        double resolutionRate = total == 0 ? 0 : (closed * 100.0) / total;

        List<Intervention> closedInterventions = interventionRepository.findByStatus(Status.CLOSED);
        Double avgHours = hoursFromMinutes(closedInterventions.stream()
                .mapToLong(i -> Duration.between(i.getCreatedAt(), i.getClosedAt()).toMinutes())
                .average());

        return new DashboardDtos.AdminStats(
                userRepository.count(),
                clientRepository.count(),
                technicianRepository.count(),
                total,
                interventionRepository.countByStatus(Status.IN_PROGRESS),
                interventionRepository.countByPriorityInAndStatusNotIn(URGENT_PRIORITIES, FINISHED),
                closed,
                round(resolutionRate, 1),
                avgHours,
                countByMonth(),
                countByCategory(),
                countByPriority(),
                technicianPerformance()
        );
    }

    @Transactional(readOnly = true)
    public DashboardDtos.ManagerStats managerStats() {
        long total = interventionRepository.count();
        long closed = interventionRepository.countByStatus(Status.CLOSED);
        return new DashboardDtos.ManagerStats(
                total,
                interventionRepository.countByStatus(Status.IN_PROGRESS),
                interventionRepository.countByPriorityInAndStatusNotIn(URGENT_PRIORITIES, FINISHED),
                closed,
                total == 0 ? 0 : round((closed * 100.0) / total, 1),
                countByPriority(),
                technicianPerformance()
        );
    }

    @Transactional(readOnly = true)
    public DashboardDtos.TechnicianStats technicianStats() {
        User user = SecurityUtils.currentUser();
        Technician technician = technicianService.findTechnicianByUserId(user.getId());
        Long techId = technician.getId();

        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        long today = interventionRepository
                .findByTechnicianIdAndCreatedAtBetweenAndStatusNot(techId, dayStart, dayEnd, Status.CLOSED).size();
        long completed = interventionRepository
                .countByTechnicianIdAndStatusIn(techId, List.of(Status.RESOLVED, Status.CLOSED));

        Double avgHours = hoursFromMinutes(interventionRepository
                .findByTechnicianIdAndActualTimeMinutesIsNotNull(techId).stream()
                .mapToLong(Intervention::getActualTimeMinutes)
                .average());

        return new DashboardDtos.TechnicianStats(
                interventionRepository.countByTechnicianId(techId),
                today,
                interventionRepository.countByTechnicianIdAndStatus(techId, Status.IN_PROGRESS),
                completed,
                avgHours,
                countByMonth()
        );
    }

    private List<DashboardDtos.TechnicianPerf> technicianPerformance() {
        Map<Long, Long> closed = aggregate(interventionRepository.countClosedByTechnician(FINISHED));
        Map<Long, Long> active = aggregate(interventionRepository.countActiveByTechnician(FINISHED));
        Map<Long, Double> avgHours = new HashMap<>();
        interventionRepository.avgActualTimeByTechnician().forEach(row -> {
            Number id = (Number) row[0];
            Number avgMinutes = (Number) row[1];
            if (avgMinutes != null) {
                avgHours.put(id.longValue(), round(avgMinutes.doubleValue() / 60.0, 2));
            }
        });

        return technicianRepository.findAll().stream()
                .map(t -> new DashboardDtos.TechnicianPerf(
                        t.getUser().getFirstName() + " " + t.getUser().getLastName(),
                        closed.getOrDefault(t.getId(), 0L),
                        active.getOrDefault(t.getId(), 0L),
                        avgHours.get(t.getId())
                ))
                .sorted((a, b) -> Long.compare(b.completed(), a.completed()))
                .toList();
    }

    private Map<Long, Long> aggregate(List<Object[]> rows) {
        Map<Long, Long> result = new HashMap<>();
        rows.forEach(row -> result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue()));
        return result;
    }

    private List<DashboardDtos.MonthCount> countByMonth() {
        return interventionRepository.countByMonth().stream()
                .map(row -> new DashboardDtos.MonthCount(
                        String.format("%04d-%02d", ((Number) row[0]).intValue(), ((Number) row[1]).intValue()),
                        ((Number) row[2]).longValue()))
                .toList();
    }

    private List<DashboardDtos.NameCount> countByCategory() {
        return interventionRepository.countByCategory().stream()
                .map(row -> new DashboardDtos.NameCount(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();
    }

    private List<DashboardDtos.NameCount> countByPriority() {
        return interventionRepository.countByPriority().stream()
                .map(row -> new DashboardDtos.NameCount(((Priority) row[0]).getLabel(), ((Number) row[1]).longValue()))
                .toList();
    }

    private Double hoursFromMinutes(java.util.OptionalDouble average) {
        return average.isPresent() ? round(average.getAsDouble() / 60.0, 2) : null;
    }

    private double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}