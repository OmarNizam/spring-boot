package com.amigoscode;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SoftwareEngineerTest {

    @Test
    void allArgsConstructorAndAccessorsRoundTrip() {
        UUID id = UUID.randomUUID();
        SoftwareEngineer engineer = new SoftwareEngineer(id, "James", List.of("java", "spring"));

        assertThat(engineer.getId()).isEqualTo(id);
        assertThat(engineer.getName()).isEqualTo("James");
        assertThat(engineer.getTechStack()).containsExactly("java", "spring");
    }

    @Test
    void nameAndTechStackConstructorLeavesIdNull() {
        SoftwareEngineer engineer = new SoftwareEngineer("Jamila", List.of("python"));

        assertThat(engineer.getId()).isNull();
        assertThat(engineer.getName()).isEqualTo("Jamila");
    }

    @Test
    void settersMutateState() {
        SoftwareEngineer engineer = new SoftwareEngineer();
        UUID id = UUID.randomUUID();

        engineer.setId(id);
        engineer.setName("Anna");
        engineer.setTechStack(List.of("go"));

        assertThat(engineer.getId()).isEqualTo(id);
        assertThat(engineer.getName()).isEqualTo("Anna");
        assertThat(engineer.getTechStack()).containsExactly("go");
    }

    /**
     * Documents the current IDE-generated {@code equals}: two engineers are equal only when
     * <em>every</em> field matches, id included. ARCHITECTURE.md flags this as fragile for a
     * JPA entity (see "Known rough edges"); this test pins the behaviour as it stands rather
     * than endorsing it.
     */
    @Test
    void equalsComparesEveryField() {
        UUID id = UUID.randomUUID();
        SoftwareEngineer engineer = new SoftwareEngineer(id, "James", List.of("java"));

        assertThat(engineer)
                .isEqualTo(new SoftwareEngineer(id, "James", List.of("java")))
                .isNotEqualTo(new SoftwareEngineer(UUID.randomUUID(), "James", List.of("java")))
                .isNotEqualTo(new SoftwareEngineer(id, "Jamila", List.of("java")))
                .isNotEqualTo(new SoftwareEngineer(id, "James", List.of("python")))
                .isNotEqualTo(null)
                .isNotEqualTo("not an engineer");
    }

    /**
     * Only the required part of the contract — equal objects share a hash code. We do not
     * assert what {@code hashCode} is built from: because it currently includes the generated
     * id, the value changes when a transient instance is persisted, which is exactly the
     * fragility ARCHITECTURE.md warns about.
     */
    @Test
    void equalEngineersShareAHashCode() {
        UUID id = UUID.randomUUID();
        SoftwareEngineer engineer = new SoftwareEngineer(id, "James", List.of("java"));

        assertThat(engineer).hasSameHashCodeAs(new SoftwareEngineer(id, "James", List.of("java")));
    }
}
