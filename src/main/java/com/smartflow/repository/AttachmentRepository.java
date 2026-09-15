package com.smartflow.repository;

import com.smartflow.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByInterventionIdOrderByUploadedAtAsc(Long interventionId);
}