package com.smartflow.repository;

import com.smartflow.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByUserId(Long userId);

    List<Client> findAllByOrderByCompanyNameAsc();
}