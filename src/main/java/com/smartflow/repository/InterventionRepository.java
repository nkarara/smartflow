package com.smartflow.repository;

import com.smartflow.entity.Intervention;
import com.smartflow.entity.Priority;
import com.smartflow.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InterventionRepository extends JpaRepository<Intervention, Long> {

    List<Intervention> findByClientIdOrderByCreatedAtDesc(Long clientId);

    List<Intervention> findByTechnicianIdOrderByCreatedAtDesc(Long technicianId);

    long countByStatus(Status status);

    long countByPriorityInAndStatusNotIn(List<Priority> priorities, List<Status> statuses);

    long countByPriorityInAndStatusNot(List<Priority> priorities, Status status);

    long countByStatusIn(List<Status> statuses);

    long countByTechnicianIdAndStatus(Long technicianId, Status status);

    long countByTechnicianIdAndStatusIn(Long technicianId, List<Status> statuses);

    List<Intervention> findByTechnicianIdAndStatusNot(Long technicianId, Status status);

    long countByStatusNot(Status status);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Intervention> findByTechnicianIdAndCreatedAtBetweenAndStatusNot(
            Long technicianId, LocalDateTime start, LocalDateTime end, Status status);

    List<Intervention> findByStatusAndTechnicianId(Long technicianId, Status status);

    @Query("""
            select year(i.createdAt) as y, month(i.createdAt) as m, count(i)
            from Intervention i
            group by year(i.createdAt), month(i.createdAt)
            order by y, m
            """)
    List<Object[]> countByMonth();

    @Query("""
            select c.name, count(i)
            from Intervention i join i.category c
            group by c.name
            order by count(i) desc
            """)
    List<Object[]> countByCategory();

    @Query("""
            select i.priority, count(i)
            from Intervention i
            group by i.priority
            order by count(i) desc
            """)
    List<Object[]> countByPriority();

    @Query("""
            select i.technician.id, count(i)
            from Intervention i
            where i.technician is not null and i.status in :closedStatuses
            group by i.technician.id
            """)
    List<Object[]> countClosedByTechnician(@Param("closedStatuses") List<Status> closedStatuses);

    @Query("""
            select i.technician.id, count(i)
            from Intervention i
            where i.technician is not null and i.status not in :finishedStatuses
            group by i.technician.id
            """)
    List<Object[]> countActiveByTechnician(@Param("finishedStatuses") List<Status> finishedStatuses);

    @Query("""
            select i.technician.id, avg(i.actualTimeMinutes)
            from Intervention i
            where i.technician is not null and i.actualTimeMinutes is not null
            group by i.technician.id
            """)
    List<Object[]> avgActualTimeByTechnician();

    List<Intervention> findByStatus(Status status);

    long countByClientId(Long clientId);

    long countByCategoryId(Long categoryId);

    long countByTechnicianId(Long technicianId);

    List<Intervention> findByTechnicianIdAndActualTimeMinutesIsNotNull(Long technicianId);

    List<Intervention> findTop10ByOrderByCreatedAtDesc();
}