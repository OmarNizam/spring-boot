package com.amigoscode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request body for creating a {@link SoftwareEngineer}.
 *
 * <p>Deliberately <em>not</em> the JPA entity: it carries no {@code id}, so a
 * client cannot supply one and turn a create into an overwrite/merge of an
 * existing row (see docs/ARCHITECTURE.md "Known rough edges"). The service maps
 * this to a fresh entity with a {@code null} id, which is what forces
 * {@code JpaRepository.save} down the insert path.
 */
public record CreateSoftwareEngineerRequest(
        @NotBlank String name,
        @NotEmpty List<@NotBlank String> techStack
) {
}
