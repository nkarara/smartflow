package com.smartflow.repository;

import com.smartflow.entity.InterventionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterventionHistoryRepository extends JpaRepository<InterventionHistory, Long> {

    List<InterventionHistory> findByInterventionIdOrderByChangedAtAsc(Long interventionId);
}