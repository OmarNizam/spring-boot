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
 * Exercises the create, update, and delete paths through the real service and Hibernate against
 * the Postgres container. Like {@link SoftwareEngineerRepositoryTest} this needs the
 * database running (host port 5332); {@code spring.docker.compose.skip.in-tests} is true
 * so the suite will not start it. {@code @Transactional} rolls each test back so the
 * {@code DataSeeder} rows stay untouched.
 *
 * <p>{@link SoftwareEngineerServiceTest} already pins the request-to-entity mapping against
 * a Mockito stub. This adds the pieces that stub cannot show: docs/ARCHITECTURE.md
 * "The write path" justifies the DTO design with "the service maps it to a fresh entity
 * with a {@code null} id, which forces the insert path" — that claim is verified here
 * end-to-end against a real persistence context (a new row appears and reloads intact),
 * not a mock. The persistence context is cleared between the insert and the reload so
 * {@code findById} issues a real SELECT rather than returning the managed instance.
 * "The delete path" claims {@code deleteById} cascades to the
 * {@code software_engineer_tech_stack} side table — verified here by counting both tables
 * before and after against real Postgres.
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

    /**
     * Verifies the update path merges onto the existing row rather than inserting a new one,
     * and that a stale {@code techStack} is fully replaced — not appended to — in the
     * {@code software_engineer_tech_stack} side table.
     */
    @Test
    void updateSoftwareEngineerById_overwritesNameAndTechStackOfTheExistingRow() {
        SoftwareEngineer created = softwareEngineerService.insertSoftwareEngineer(
                new CreateSoftwareEngineerRequest("Elin", List.of("kotlin"))
        );
        softwareEngineerRepository.flush();
        UUID id = created.getId();
        long before = softwareEngineerRepository.count();

        SoftwareEngineer updated = softwareEngineerService.updateSoftwareEngineerById(
                id, new UpdateSoftwareEngineerRequest("Elin Updated", List.of("kotlin", "ktor"))
        ).orElseThrow();
        softwareEngineerRepository.flush();
        entityManager.clear();

        assertThat(updated.getId()).isEqualTo(id);
        assertThat(softwareEngineerRepository.count()).isEqualTo(before);

        SoftwareEngineer reloaded = softwareEngineerRepository.findById(id).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Elin Updated");
        assertThat(reloaded.getTechStack()).containsExactlyInAnyOrder("kotlin", "ktor");
    }

    @Test
    void updateSoftwareEngineerById_returnsEmptyWhenNoSuchRow() {
        assertThat(softwareEngineerService.updateSoftwareEngineerById(
                UUID.randomUUID(), new UpdateSoftwareEngineerRequest("Nobody", List.of("java"))
        )).isEmpty();
    }

    /**
     * The service method returns a plain boolean; only a real persistence context shows
     * what {@code deleteById} actually does to the {@code software_engineer_tech_stack}
     * side table. Verifies the row and its {@code @ElementCollection} entries both go —
     * no orphaned tech rows — and that a second delete of the same id reports {@code false}.
     */
    @Test
    void deleteSoftwareEngineerById_removesTheRowAndItsTechStackSideTable() {
        SoftwareEngineer created = softwareEngineerService.insertSoftwareEngineer(
                new CreateSoftwareEngineerRequest("Dana", List.of("rust", "wasm"))
        );
        softwareEngineerRepository.flush();
        UUID id = created.getId();

        Number techBefore = (Number) entityManager.createNativeQuery(
                        "select count(*) from software_engineer_tech_stack where software_engineer_id = :id")
                .setParameter("id", id).getSingleResult();
        assertThat(techBefore.longValue()).isEqualTo(2);

        assertThat(softwareEngineerService.deleteSoftwareEngineerById(id)).isTrue();
        softwareEngineerRepository.flush();
        entityManager.clear();

        assertThat(softwareEngineerRepository.findById(id)).isEmpty();
        Number techAfter = (Number) entityManager.createNativeQuery(
                        "select count(*) from software_engineer_tech_stack where software_engineer_id = :id")
                .setParameter("id", id).getSingleResult();
        assertThat(techAfter.longValue()).isZero();

        assertThat(softwareEngineerService.deleteSoftwareEngineerById(id)).isFalse();
    }
}
