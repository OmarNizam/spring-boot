package com.amigoscode;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SoftwareEngineerRepository extends JpaRepository<SoftwareEngineer, UUID> {
}
