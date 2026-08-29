package com.amigoscode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the JPA mapping against the real Postgres container. Like {@link ApplicationTests}
 * this needs the database running, and it assumes the schema (including the
 * {@code software_engineer_tech_stack} side table) already exists — Hibernate's
 * {@code ddl-auto=update} creates any missing tables at context startup. {@code @Transactional}
 * rolls each test back so the {@code DataSeeder} rows stay untouched.
 */
@SpringBootTest
@Transactional
class SoftwareEngineerRepositoryTest {

    @Autowired
    private SoftwareEngineerRepository softwareEngineerRepository;

    @Test
    void savingAssignsAGeneratedUuid() {
        SoftwareEngineer saved = softwareEngineerRepository.save(
                new SoftwareEngineer("Anna", List.of("kotlin"))
        );

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void techStackRoundTripsThroughTheSideTable() {
        UUID id = softwareEngineerRepository.save(
                new SoftwareEngineer("Bilal", List.of("rust", "wasm", "go"))
        ).getId();
        softwareEngineerRepository.flush();

        SoftwareEngineer reloaded = softwareEngineerRepository.findById(id).orElseThrow();

        assertThat(reloaded.getName()).isEqualTo("Bilal");
        assertThat(reloaded.getTechStack()).containsExactlyInAnyOrder("rust", "wasm", "go");
    }
}
