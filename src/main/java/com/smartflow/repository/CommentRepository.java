package com.smartflow.repository;

import com.smartflow.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByInterventionIdOrderByCreatedAtAsc(Long interventionId);

    long countByInterventionId(Long interventionId);
}