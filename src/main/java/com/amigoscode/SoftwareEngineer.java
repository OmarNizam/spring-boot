package com.amigoscode;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static jakarta.persistence.GenerationType.UUID;

@Setter
@Getter
@Entity
public class SoftwareEngineer {
    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "software_engineer_tech_stack",
            joinColumns = @JoinColumn(name = "software_engineer_id")
    )
    @Column(name = "tech")
    private List<String> techStack;

    public SoftwareEngineer() {
    }

    public SoftwareEngineer(String name, List<String> techStack) {
        this.name = name;
        this.techStack = techStack;
    }

    public SoftwareEngineer(UUID id, String name, List<String> techStack) {
        this.id = id;
        this.name = name;
        this.techStack = techStack;
    }

    /*
    Generated equals() and hashCode()
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SoftwareEngineer that = (SoftwareEngineer) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(techStack, that.techStack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, techStack);
    }
}
