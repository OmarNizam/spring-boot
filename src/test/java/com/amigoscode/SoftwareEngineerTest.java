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

    @Test
    void equalsAndHashCodeCoverAllFields() {
        UUID id = UUID.randomUUID();
        SoftwareEngineer a = new SoftwareEngineer(id, "James", List.of("java"));
        SoftwareEngineer sameValues = new SoftwareEngineer(id, "James", List.of("java"));

        assertThat(a)
                .isEqualTo(sameValues)
                .hasSameHashCodeAs(sameValues)
                .isNotEqualTo(new SoftwareEngineer(UUID.randomUUID(), "James", List.of("java")))
                .isNotEqualTo(new SoftwareEngineer(id, "Jamila", List.of("java")))
                .isNotEqualTo(new SoftwareEngineer(id, "James", List.of("python")))
                .isNotEqualTo(null)
                .isNotEqualTo("not an engineer");
    }
}
