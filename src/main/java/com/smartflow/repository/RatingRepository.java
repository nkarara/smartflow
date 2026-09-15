package com.smartflow.repository;

import com.smartflow.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByInterventionId(Long interventionId);

    boolean existsByInterventionId(Long interventionId);
}