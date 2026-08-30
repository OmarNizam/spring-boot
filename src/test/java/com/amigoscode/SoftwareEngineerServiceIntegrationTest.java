package com.amigoscode;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the create path through the real service and Hibernate against the Postgres
 * container. Like {@link SoftwareEngineerRepositoryTest} this needs the database running
 * (host port 5332); {@code spring.docker.compose.skip.in-tests} is true so the suite will
 * not start it. {@code @Transactional} rolls each test back so the {@code DataSeeder} rows
 * stay untouched.
 *
 * <p>{@link SoftwareEngineerServiceTest} already pins the request-to-entity mapping against
 * a Mockito stub. This adds the piece that stub cannot show: docs/ARCHITECTURE.md
 * "The write path" justifies the DTO design with "the service maps it to a fresh entity
 * with a {@code null} id, which forces the insert path" — that claim is verified here
 * end-to-end against a real persistence context (a new row appears and reloads intact),
 * not a mock. The persistence context is cleared between the insert and the reload so
 * {@code findById} issues a real SELECT rather than returning the managed instance.
 */
@SpringBootTest
@Transactional
class SoftwareEngineerServiceIntegrationTest {

    @Autowired
    private SoftwareEngineerService softwareEngineerService;

    @Autowired
    private SoftwareEngineerRepository softwareEngineerRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void insertSoftwareEngineer_insertsANewRowThatReloadsWithAGeneratedId() {
        long before = softwareEngineerRepository.count();

        SoftwareEngineer created = softwareEngineerService.insertSoftwareEngineer(
                new CreateSoftwareEngineerRequest("Carla", List.of("scala", "zio"))
        );
        softwareEngineerRepository.flush();
        // Drop the first-level cache so findById below is a real SELECT, not a
        // handback of the still-managed `created` instance.
        entityManager.clear();

        assertThat(created.getId()).isNotNull();
        assertThat(softwareEngineerRepository.count()).isEqualTo(before + 1);

        UUID id = created.getId();
        SoftwareEngineer reloaded = softwareEngineerRepository.findById(id).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Carla");
        assertThat(reloaded.getTechStack()).containsExactlyInAnyOrder("scala", "zio");
    }
}
